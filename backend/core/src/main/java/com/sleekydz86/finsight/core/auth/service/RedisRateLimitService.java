package com.sleekydz86.finsight.core.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class RedisRateLimitService implements RateLimitServiceInterface {

    @Autowired(required = false)
    @Qualifier("customStringRedisTemplate")
    private RedisTemplate<String, String> redisTemplate;

    @Autowired(required = false)
    private RedisConnectionFactory redisConnectionFactory;

    @Value("${custom.security.rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    @Value("${custom.security.rate-limit.burst-capacity:100}")
    private int burstCapacity;

    @Value("${custom.security.rate-limit.max-violations-before-block:5}")
    private int maxViolationsBeforeBlock;

    @Value("${custom.security.rate-limit.block-duration-minutes:30}")
    private long defaultBlockDurationMinutes;

    @Value("${custom.security.rate-limit.violation-window-minutes:60}")
    private long violationWindowMinutes;

    @Value("${custom.redis.rate-limit-key-prefix:rate_limit:}")
    private String keyPrefix;

    @Value("${custom.redis.enabled:false}")
    private boolean redisEnabled;

    @Value("${custom.security.rate-limit.memory-cache-limit:1000}")
    private int memoryCacheLimit;

    private final AtomicBoolean useRedis = new AtomicBoolean(false);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicInteger cleanupCount = new AtomicInteger(0);

    private final ConcurrentHashMap<String, RateLimitInfo> memoryCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BlockInfo> blockedIdentifiers = new ConcurrentHashMap<>();
    private final Set<String> whitelistedIdentifiers = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, ViolationTracker> violationTrackers = new ConcurrentHashMap<>();

    private static final int MAX_CONSECUTIVE_FAILURES = 5;
    private static final long HEALTH_CHECK_TIMEOUT_MS = 2000;

    private static final String RATE_LIMIT_SCRIPT = "local key = KEYS[1] " +
            "local limit = tonumber(ARGV[1]) " +
            "local window = tonumber(ARGV[2]) " +
            "local current = redis.call('GET', key) " +
            "if current == false then " +
            "  redis.call('SETEX', key, window, '1') " +
            "  return 1 " +
            "end " +
            "local count = tonumber(current) " +
            "if count >= limit then " +
            "  return -1 " +
            "end " +
            "redis.call('INCR', key) " +
            "return count + 1";

    private static final String BLOCK_SCRIPT = "local blockKey = KEYS[1] " +
            "local reason = ARGV[1] " +
            "local expiry = ARGV[2] " +
            "redis.call('SETEX', blockKey, expiry, reason) " +
            "return 1";

    private static final String UNBLOCK_SCRIPT = "local blockKey = KEYS[1] " +
            "redis.call('DEL', blockKey) " +
            "return 1";

    @PostConstruct
    public void init() {
        if (redisEnabled && redisTemplate != null) {
            initializeRedisConnection();
        } else {
            useRedis.set(false);
        }
    }

    private void initializeRedisConnection() {
        try {
            if (checkRedisConnection()) {
                useRedis.set(true);
                consecutiveFailures.set(0);
                log.info("Redis 연결 성공 - Redis 기반 rate limiting을 사용합니다.");
            } else {
                switchToMemoryFallback("Redis 연결 실패");
            }
        } catch (Exception e) {
            log.warn("Redis 초기화 실패: {}", e.getMessage());
            switchToMemoryFallback("Redis 초기화 실패");
        }
    }

    @Override
    public boolean isRedisAvailable() {
        return useRedis.get() && checkRedisConnection();
    }

    @Override
    public void checkRateLimit(String identifier, String action) {
        if (isWhitelisted(identifier)) {
            log.debug("화이트리스트된 식별자 {} - rate limiting 건너뜀", identifier);
            return;
        }

        if (isIdentifierBlocked(identifier)) {
            BlockInfo blockInfo = blockedIdentifiers.get(identifier);
            String errorMsg = String.format("식별자 %s가 차단됨. 사유: %s, 만료시간: %s",
                    identifier, blockInfo.getReason(),
                    LocalDateTime.ofEpochSecond(blockInfo.getExpiryTime(), 0, java.time.ZoneOffset.UTC));
            log.warn(errorMsg);
            throw new SecurityException(errorMsg);
        }

        if (useRedis.get()) {
            try {
                checkRateLimitWithRedis(identifier, action);
            } catch (Exception e) {
                handleRedisFailure("Rate limit 확인 실패", identifier, action, e);
                checkRateLimitWithMemory(identifier, action);
            }
        } else {
            checkRateLimitWithMemory(identifier, action);
        }
    }

    @Override
    public int getCurrentCount(String identifier, String action) {
        if (useRedis.get()) {
            try {
                return getCurrentCountWithRedis(identifier, action);
            } catch (Exception e) {
                log.warn("Redis에서 카운트 조회 실패, 메모리에서 조회: {}", e.getMessage());
                return getCurrentCountWithMemory(identifier, action);
            }
        }
        return getCurrentCountWithMemory(identifier, action);
    }

    @Override
    public void resetLimit(String identifier, String action) {
        if (useRedis.get()) {
            try {
                resetLimitWithRedis(identifier, action);
            } catch (Exception e) {
                log.warn("Redis에서 limit reset 실패, 메모리에서 처리: {}", e.getMessage());
                resetLimitWithMemory(identifier, action);
            }
        } else {
            resetLimitWithMemory(identifier, action);
        }
    }

    @Override
    public void blockIdentifier(String identifier, String reason, long blockDurationMinutes) {
        if (useRedis.get()) {
            try {
                blockIdentifierWithRedis(identifier, reason, blockDurationMinutes);
            } catch (Exception e) {
                log.warn("Redis에서 차단 처리 실패, 메모리에서 처리: {}", e.getMessage());
                blockIdentifierWithMemory(identifier, reason, blockDurationMinutes);
            }
        } else {
            blockIdentifierWithMemory(identifier, reason, blockDurationMinutes);
        }
    }

    @Override
    public void unblockIdentifier(String identifier) {
        if (useRedis.get()) {
            try {
                unblockIdentifierWithRedis(identifier);
            } catch (Exception e) {
                log.warn("Redis에서 차단 해제 실패, 메모리에서 처리: {}", e.getMessage());
                unblockIdentifierWithMemory(identifier);
            }
        } else {
            unblockIdentifierWithMemory(identifier);
        }
    }

    @Override
    public boolean isIdentifierBlocked(String identifier) {
        if (useRedis.get()) {
            try {
                return isIdentifierBlockedWithRedis(identifier);
            } catch (Exception e) {
                log.warn("Redis에서 차단 상태 확인 실패, 메모리에서 확인: {}", e.getMessage());
                return isIdentifierBlockedWithMemory(identifier);
            }
        }
        return isIdentifierBlockedWithMemory(identifier);
    }

    @Override
    public void addToWhitelist(String identifier) {
        if (useRedis.get()) {
            try {
                addToWhitelistWithRedis(identifier);
            } catch (Exception e) {
                log.warn("Redis에서 화이트리스트 추가 실패, 메모리에서 처리: {}", e.getMessage());
                addToWhitelistWithMemory(identifier);
            }
        } else {
            addToWhitelistWithMemory(identifier);
        }
    }

    @Override
    public void removeFromWhitelist(String identifier) {
        if (useRedis.get()) {
            try {
                removeFromWhitelistWithRedis(identifier);
            } catch (Exception e) {
                log.warn("Redis에서 화이트리스트 제거 실패, 메모리에서 처리: {}", e.getMessage());
                removeFromWhitelistWithMemory(identifier);
            }
        } else {
            removeFromWhitelistWithMemory(identifier);
        }
    }

    @Override
    public boolean isWhitelisted(String identifier) {
        if (useRedis.get()) {
            try {
                return isWhitelistedWithRedis(identifier);
            } catch (Exception e) {
                log.warn("Redis에서 화이트리스트 확인 실패, 메모리에서 확인: {}", e.getMessage());
                return isWhitelistedWithMemory(identifier);
            }
        }
        return isWhitelistedWithMemory(identifier);
    }

    @Override
    public RateLimitSecurityInfo getSecurityInfo(String identifier) {
        if (useRedis.get()) {
            try {
                return getSecurityInfoWithRedis(identifier);
            } catch (Exception e) {
                log.warn("Redis에서 보안 정보 조회 실패, 메모리에서 조회: {}", e.getMessage());
                return getSecurityInfoWithMemory(identifier);
            }
        }
        return getSecurityInfoWithMemory(identifier);
    }

    private void checkRateLimitWithRedis(String identifier, String action) {
        String key = generateKey(identifier, action);
        RedisScript<Long> script = new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, Long.class);

        List<String> keys = Arrays.asList(key);
        List<String> args = Arrays.asList(String.valueOf(requestsPerMinute), "60");

        Long result = redisTemplate.execute(script, keys, args.toArray());
        if (result == null || result < 0) {
            trackViolation(identifier, action);
            if (shouldAutoBlock(identifier)) {
                blockIdentifier(identifier, "자동 차단: Rate limit 위반 횟수 초과", defaultBlockDurationMinutes);
            }
            throw new SecurityException("Rate limit exceeded for identifier: " + identifier);
        }
    }

    private void blockIdentifierWithRedis(String identifier, String reason, long blockDurationMinutes) {
        String blockKey = keyPrefix + "block:" + identifier;
        RedisScript<Long> script = new DefaultRedisScript<>(BLOCK_SCRIPT, Long.class);

        List<String> keys = Arrays.asList(blockKey);
        List<String> args = Arrays.asList(reason, String.valueOf(blockDurationMinutes * 60));
        redisTemplate.execute(script, keys, args.toArray());
        log.info("식별자 {}가 Redis에서 차단됨", identifier);
    }

    private void unblockIdentifierWithRedis(String identifier) {
        String blockKey = keyPrefix + "block:" + identifier;
        RedisScript<Long> script = new DefaultRedisScript<>(UNBLOCK_SCRIPT, Long.class);

        List<String> keys = Arrays.asList(blockKey);
        List<String> args = Arrays.asList();
        redisTemplate.execute(script, keys, args.toArray());
        log.info("식별자 {}의 차단이 Redis에서 해제됨", identifier);
    }

    private boolean isIdentifierBlockedWithRedis(String identifier) {
        String blockKey = keyPrefix + "block:" + identifier;
        String blockInfo = redisTemplate.opsForValue().get(blockKey);
        return blockInfo != null;
    }

    private void addToWhitelistWithRedis(String identifier) {
        String whitelistKey = keyPrefix + "whitelist:" + identifier;
        redisTemplate.opsForValue().set(whitelistKey, "1");
        log.info("식별자 {}가 Redis 화이트리스트에 추가됨", identifier);
    }

    private void removeFromWhitelistWithRedis(String identifier) {
        String whitelistKey = keyPrefix + "whitelist:" + identifier;
        redisTemplate.delete(whitelistKey);
        log.info("식별자 {}가 Redis 화이트리스트에서 제거됨", identifier);
    }

    private boolean isWhitelistedWithRedis(String identifier) {
        String whitelistKey = keyPrefix + "whitelist:" + identifier;
        String result = redisTemplate.opsForValue().get(whitelistKey);
        return result != null;
    }

    private RateLimitSecurityInfo getSecurityInfoWithRedis(String identifier) {
        String blockKey = keyPrefix + "block:" + identifier;
        String whitelistKey = keyPrefix + "whitelist:" + identifier;

        String blockReason = redisTemplate.opsForValue().get(blockKey);
        String whitelistStatus = redisTemplate.opsForValue().get(whitelistKey);

        boolean isBlocked = blockReason != null;
        boolean isWhitelisted = whitelistStatus != null;

        return new RateLimitSecurityInfo(isBlocked, blockReason, 0, isWhitelisted, 0, 0);
    }

    private void checkRateLimitWithMemory(String identifier, String action) {
        String key = generateKey(identifier, action);
        RateLimitInfo info = memoryCache.computeIfAbsent(key, k -> new RateLimitInfo(0, LocalDateTime.now()));

        LocalDateTime now = LocalDateTime.now();

        if (info.getCreatedAt().plusMinutes(1).isBefore(now)) {
            info.reset(now);
        }

        if (info.getCount() >= requestsPerMinute) {
            trackViolation(identifier, action);

            if (shouldAutoBlock(identifier)) {
                blockIdentifier(identifier, "자동 차단: Rate limit 위반 횟수 초과", defaultBlockDurationMinutes);
                log.warn("식별자 {}가 자동으로 차단됨. 위반 횟수: {}", identifier, getViolationCount(identifier));
            }

            throw new SecurityException("Rate limit exceeded for identifier: " + identifier);
        }

        info.incrementCount();
    }

    private void blockIdentifierWithMemory(String identifier, String reason, long blockDurationMinutes) {
        long expiryTime = System.currentTimeMillis() / 1000 + (blockDurationMinutes * 60);
        BlockInfo blockInfo = new BlockInfo(reason, expiryTime, LocalDateTime.now());
        blockedIdentifiers.put(identifier, blockInfo);

        cleanupIdentifierData(identifier);
        log.warn("식별자 {}가 메모리에서 차단됨. 사유: {}, 만료시간: {}",
                identifier, reason,
                LocalDateTime.ofEpochSecond(expiryTime, 0, java.time.ZoneOffset.UTC));
    }

    private void unblockIdentifierWithMemory(String identifier) {
        BlockInfo removed = blockedIdentifiers.remove(identifier);
        if (removed != null) {
            log.info("식별자 {}의 차단이 메모리에서 해제됨", identifier);
        }
    }

    private boolean isIdentifierBlockedWithMemory(String identifier) {
        BlockInfo blockInfo = blockedIdentifiers.get(identifier);
        if (blockInfo == null) {
            return false;
        }

        if (System.currentTimeMillis() / 1000 > blockInfo.getExpiryTime()) {
            blockedIdentifiers.remove(identifier);
            return false;
        }

        return true;
    }

    private void addToWhitelistWithMemory(String identifier) {
        whitelistedIdentifiers.add(identifier);
        log.info("식별자 {}가 메모리 화이트리스트에 추가됨", identifier);
    }

    private void removeFromWhitelistWithMemory(String identifier) {
        whitelistedIdentifiers.remove(identifier);
        log.info("식별자 {}가 메모리 화이트리스트에서 제거됨", identifier);
    }

    private boolean isWhitelistedWithMemory(String identifier) {
        return whitelistedIdentifiers.contains(identifier);
    }

    private RateLimitSecurityInfo getSecurityInfoWithMemory(String identifier) {
        BlockInfo blockInfo = blockedIdentifiers.get(identifier);
        ViolationTracker tracker = violationTrackers.get(identifier);

        boolean isBlocked = isIdentifierBlockedWithMemory(identifier);
        String blockReason = isBlocked ? blockInfo.getReason() : null;
        long blockExpiryTime = isBlocked ? blockInfo.getExpiryTime() : 0;
        boolean isWhitelisted = isWhitelistedWithMemory(identifier);
        int violationCount = tracker != null ? tracker.getViolationCount() : 0;
        long lastViolationTime = tracker != null ? tracker.getLastViolationTime() : 0;

        return new RateLimitSecurityInfo(isBlocked, blockReason, blockExpiryTime,
                isWhitelisted, violationCount, lastViolationTime);
    }

    private void trackViolation(String identifier, String action) {
        ViolationTracker tracker = violationTrackers.computeIfAbsent(identifier,
                k -> new ViolationTracker());
        tracker.recordViolation();

        log.warn("Rate limit 위반 기록: 식별자 {}, 액션 {}, 총 위반 횟수: {}",
                identifier, action, tracker.getViolationCount());
    }

    private boolean shouldAutoBlock(String identifier) {
        ViolationTracker tracker = violationTrackers.get(identifier);
        if (tracker == null) {
            return false;
        }

        long windowStart = System.currentTimeMillis() - (violationWindowMinutes * 60 * 1000);
        int violationsInWindow = tracker.getViolationsInWindow(windowStart);

        return violationsInWindow >= maxViolationsBeforeBlock;
    }

    private int getViolationCount(String identifier) {
        ViolationTracker tracker = violationTrackers.get(identifier);
        return tracker != null ? tracker.getViolationCount() : 0;
    }

    private void cleanupIdentifierData(String identifier) {
        memoryCache.entrySet().removeIf(entry -> entry.getKey().startsWith(identifier + ":"));
        violationTrackers.remove(identifier);
    }

    private int getCurrentCountWithMemory(String identifier, String action) {
        String key = generateKey(identifier, action);
        RateLimitInfo info = memoryCache.get(key);
        return info != null ? info.getCount() : 0;
    }

    private void resetLimitWithMemory(String identifier, String action) {
        String key = generateKey(identifier, action);
        RateLimitInfo info = memoryCache.get(key);
        if (info != null) {
            info.reset(LocalDateTime.now());
            log.info("Rate limit reset for identifier: {} action: {} (memory)", identifier, action);
        }
    }

    private void handleRedisFailure(String message, String identifier, String action, Exception e) {
        consecutiveFailures.incrementAndGet();
        log.error("{} - identifier: {}, action: {}, 오류: {}", message, identifier, action, e.getMessage());

        if (consecutiveFailures.get() >= MAX_CONSECUTIVE_FAILURES) {
            switchToMemoryFallback("연속적인 Redis 연결 실패");
        }
    }

    private void switchToMemoryFallback(String reason) {
        useRedis.set(false);
        log.warn("Redis에서 메모리 fallback으로 전환: {}", reason);

        if (memoryCache.size() > memoryCacheLimit) {
            cleanupMemoryCache();
        }
    }

    private void cleanupMemoryCache() {
        int beforeSize = memoryCache.size();
        memoryCache.clear();
        blockedIdentifiers.clear();
        whitelistedIdentifiers.clear();
        violationTrackers.clear();
        log.info("메모리 캐시 정리 완료: {} -> 0", beforeSize);
    }

    private boolean checkRedisConnection() {
        try {
            if (redisConnectionFactory != null) {
                try (var connection = redisConnectionFactory.getConnection()) {
                    connection.ping();
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.debug("Redis 연결 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    private String generateKey(String identifier, String action) {
        return identifier + ":" + action;
    }

    private static class RateLimitInfo {
        private final AtomicInteger count;
        private final AtomicReference<LocalDateTime> createdAt;

        RateLimitInfo(int count, LocalDateTime createdAt) {
            this.count = new AtomicInteger(count);
            this.createdAt = new AtomicReference<>(createdAt);
        }

        void incrementCount() {
            count.incrementAndGet();
        }

        int getCount() {
            return count.get();
        }

        LocalDateTime getCreatedAt() {
            return createdAt.get();
        }

        void reset(LocalDateTime newTime) {
            count.set(0);
            createdAt.set(newTime);
        }
    }

    private static class BlockInfo {
        private final String reason;
        private final long expiryTime;
        private final LocalDateTime blockedAt;

        BlockInfo(String reason, long expiryTime, LocalDateTime blockedAt) {
            this.reason = reason;
            this.expiryTime = expiryTime;
            this.blockedAt = blockedAt;
        }

        String getReason() { return reason; }
        long getExpiryTime() { return expiryTime; }
        LocalDateTime getBlockedAt() { return blockedAt; }
    }

    private static class ViolationTracker {
        private final AtomicInteger violationCount = new AtomicInteger(0);
        private final AtomicLong lastViolationTime = new AtomicLong(0);
        private final ConcurrentHashMap<Long, Integer> violationHistory = new ConcurrentHashMap<>();

        void recordViolation() {
            long now = System.currentTimeMillis();
            violationCount.incrementAndGet();
            lastViolationTime.set(now);

            long minuteKey = now / (60 * 1000);
            violationHistory.compute(minuteKey, (k, v) -> (v == null) ? 1 : v + 1);

            long cutoff = (now - (60 * 60 * 1000)) / (60 * 1000);
            violationHistory.entrySet().removeIf(entry -> entry.getKey() < cutoff);
        }

        int getViolationCount() {
            return violationCount.get();
        }

        long getLastViolationTime() {
            return lastViolationTime.get();
        }

        int getViolationsInWindow(long windowStart) {
            long windowStartMinute = windowStart / (60 * 1000);
            return violationHistory.entrySet().stream()
                    .filter(entry -> entry.getKey() >= windowStartMinute)
                    .mapToInt(Map.Entry::getValue)
                    .sum();
        }
    }

    private int getCurrentCountWithRedis(String identifier, String action) {
        String key = generateKey(identifier, action);
        String count = redisTemplate.opsForValue().get(key);
        return count != null ? Integer.parseInt(count) : 0;
    }

    private void resetLimitWithRedis(String identifier, String action) {
        String key = generateKey(identifier, action);
        redisTemplate.delete(key);
        log.info("Rate limit reset for identifier: {} action: {} (Redis)", identifier, action);
    }
}