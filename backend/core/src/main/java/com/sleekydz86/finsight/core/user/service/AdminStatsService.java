package com.sleekydz86.finsight.core.user.service;

import com.sleekydz86.finsight.core.board.adapter.persistence.command.BoardJpaRepository;
import com.sleekydz86.finsight.core.comment.adapter.persistence.command.CommentJpaRepository;
import com.sleekydz86.finsight.core.global.exception.ValidationException;
import com.sleekydz86.finsight.core.health.domain.Health;
import com.sleekydz86.finsight.core.health.domain.port.in.HealthCommandUseCase;
import com.sleekydz86.finsight.core.health.domain.port.in.HealthQueryUseCase;
import com.sleekydz86.finsight.core.health.domain.vo.HealthStatus;
import com.sleekydz86.finsight.core.health.domain.vo.SystemMetrics;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaRepository;
import com.sleekydz86.finsight.core.user.adapter.persistence.command.UserJpaRepository;
import com.sleekydz86.finsight.core.user.domain.UserStatus;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.admin.AdminStatsChartResponse;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.admin.AdminStatsNamedSeries;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.admin.AdminStatsOverviewResponse;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.admin.AdminStatsSeriesPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatsService {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int DEFAULT_DAYS = 7;
    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 90;

    private final UserJpaRepository userJpaRepository;
    private final BoardJpaRepository boardJpaRepository;
    private final CommentJpaRepository commentJpaRepository;
    private final NewsJpaRepository newsJpaRepository;
    private final HealthQueryUseCase healthQueryUseCase;
    private final HealthCommandUseCase healthCommandUseCase;

    public AdminStatsOverviewResponse overview() {
        log.info("관리자 통계 개요 조회");
        Map<String, Object> healthSnapshot = buildHealthSnapshot();
        Map<String, Object> metricsSnapshot = buildMetricsSnapshot();
        return new AdminStatsOverviewResponse(
                userJpaRepository.count(),
                userJpaRepository.countByStatus(UserStatus.APPROVED),
                userJpaRepository.countByStatus(UserStatus.PENDING),
                userJpaRepository.countByStatus(UserStatus.SUSPENDED),
                userJpaRepository.countByStatus(UserStatus.WITHDRAWN),
                boardJpaRepository.count(),
                commentJpaRepository.count(),
                newsJpaRepository.count(),
                healthSnapshot,
                metricsSnapshot);
    }

    public AdminStatsChartResponse chart(String chartKey, Integer days) {
        return chart(chartKey, days, null, null);
    }

    public AdminStatsChartResponse chart(String chartKey, Integer days, LocalDate fromDate, LocalDate toDate) {
        String key = chartKey == null ? "" : chartKey.trim().toLowerCase(Locale.ROOT);
        LocalDate to = toDate != null ? toDate : LocalDate.now();
        LocalDate from;
        if (fromDate != null) {
            from = fromDate;
        } else {
            int resolvedDays = clampDays(days);
            from = to.minusDays(resolvedDays - 1L);
        }
        if (from.isAfter(to)) {
            LocalDate swap = from;
            from = to;
            to = swap;
        }
        long span = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
        if (span > MAX_DAYS) {
            from = to.minusDays(MAX_DAYS - 1L);
        }
        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toExclusive = to.plusDays(1).atStartOfDay();

        log.info("관리자 통계 차트 조회 - key={}, from={}, to={}", key, from, to);

        return switch (key) {
            case "signups" -> buildDailyChart(
                    "signups",
                    "신규 가입 / 탈퇴",
                    "명",
                    from,
                    to,
                    List.of(
                            namedDailySeries(
                                    "signups",
                                    "신규 가입",
                                    from,
                                    to,
                                    toDayCountMap(userJpaRepository.countSignupsByDay(fromDateTime, toExclusive))),
                            namedDailySeries(
                                    "withdrawn",
                                    "탈퇴",
                                    from,
                                    to,
                                    toDayCountMap(userJpaRepository.countWithdrawnByUpdatedDay(fromDateTime, toExclusive)))));
            case "logins" -> buildDailyChart(
                    "logins",
                    "활동 사용자",
                    "명",
                    from,
                    to,
                    List.of(namedDailySeries(
                            "logins",
                            "일간 로그인",
                            from,
                            to,
                            toDayCountMap(userJpaRepository.countLoginsByDay(fromDateTime, toExclusive)))));
            case "cumulative" -> buildCumulativeChart(from, to, fromDateTime, toExclusive);
            case "providers" -> buildProvidersChart(from, to);
            case "status" -> buildStatusChart(from, to);
            case "content" -> buildDailyChart(
                    "content",
                    "게시글 / 댓글",
                    "건",
                    from,
                    to,
                    List.of(
                            namedDailySeries(
                                    "boards",
                                    "게시글",
                                    from,
                                    to,
                                    toDayCountMap(boardJpaRepository.countCreatedByDay(fromDateTime, toExclusive))),
                            namedDailySeries(
                                    "comments",
                                    "댓글",
                                    from,
                                    to,
                                    toDayCountMap(commentJpaRepository.countCreatedByDay(fromDateTime, toExclusive)))));
            case "news" -> buildDailyChart(
                    "news",
                    "뉴스 수집",
                    "건",
                    from,
                    to,
                    List.of(namedDailySeries(
                            "news",
                            "뉴스",
                            from,
                            to,
                            toDayCountMap(newsJpaRepository.countCreatedByDay(fromDateTime, toExclusive)))));
            case "health" -> buildHealthChart(from, to, fromDateTime, toExclusive);
            case "metrics" -> buildMetricsChart(from, to, fromDateTime, toExclusive);
            default -> throw new ValidationException(
                    "지원하지 않는 차트 키입니다: " + chartKey,
                    List.of("chartKey"));
        };
    }

    @Transactional
    public Health refreshHealth() {
        log.info("관리자 헬스 상태 새로고침");
        Health health = healthQueryUseCase.getCompleteHealth();
        return healthCommandUseCase.saveHealthCheck(health);
    }

    private AdminStatsChartResponse buildCumulativeChart(
            LocalDate from,
            LocalDate to,
            LocalDateTime fromDateTime,
            LocalDateTime toExclusive) {
        Map<LocalDate, Long> signups = toDayCountMap(
                userJpaRepository.countSignupsByDay(fromDateTime, toExclusive));
        long running = userJpaRepository.countCreatedBefore(fromDateTime);
        List<AdminStatsSeriesPoint> points = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            running += signups.getOrDefault(day, 0L);
            points.add(new AdminStatsSeriesPoint(day.format(DAY_FORMAT), running));
        }
        return buildDailyChart(
                "cumulative",
                "누적 가입",
                "명",
                from,
                to,
                List.of(new AdminStatsNamedSeries("cumulative", "누적 가입", points)));
    }

    private AdminStatsChartResponse buildProvidersChart(LocalDate from, LocalDate to) {
        List<Object[]> rows = userJpaRepository.countByAuthProvider();
        List<AdminStatsNamedSeries> series = new ArrayList<>();
        for (Object[] row : rows) {
            String provider = stringify(row[0]);
            if (provider == null || provider.isBlank()) {
                provider = "UNKNOWN";
            }
            long count = toLong(row[1]);
            String label = switch (provider.toUpperCase(Locale.ROOT)) {
                case "WEB" -> "웹";
                case "NAVER" -> "네이버";
                case "KAKAO" -> "카카오";
                case "GOOGLE" -> "구글";
                default -> provider;
            };
            series.add(new AdminStatsNamedSeries(
                    provider.toLowerCase(Locale.ROOT),
                    label,
                    List.of(new AdminStatsSeriesPoint(label, count))));
        }
        return new AdminStatsChartResponse("providers", "가입 경로별 사용자", "명", series, from, to);
    }

    private AdminStatsChartResponse buildStatusChart(LocalDate from, LocalDate to) {
        List<Object[]> rows = userJpaRepository.countGroupedByStatus();
        List<AdminStatsNamedSeries> series = new ArrayList<>();
        for (Object[] row : rows) {
            String status = stringify(row[0]);
            if (status == null || status.isBlank()) {
                status = "UNKNOWN";
            }
            long count = toLong(row[1]);
            String label = switch (status.toUpperCase(Locale.ROOT)) {
                case "PENDING" -> "승인 대기";
                case "APPROVED" -> "정상";
                case "REJECTED" -> "거부";
                case "SUSPENDED" -> "정지";
                case "WITHDRAWN" -> "탈퇴";
                default -> status;
            };
            series.add(new AdminStatsNamedSeries(
                    status.toLowerCase(Locale.ROOT),
                    label,
                    List.of(new AdminStatsSeriesPoint(label, count))));
        }
        return new AdminStatsChartResponse("status", "상태별 사용자", "명", series, from, to);
    }

    private AdminStatsChartResponse buildHealthChart(
            LocalDate from,
            LocalDate to,
            LocalDateTime fromDateTime,
            LocalDateTime toExclusive) {
        List<Health> history = healthCommandUseCase.getHealthHistoryByDateRange(fromDateTime, toExclusive);
        Map<LocalDate, long[]> dayCounts = new LinkedHashMap<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            dayCounts.put(day, new long[] { 0L, 0L });
        }
        for (Health health : history) {
            if (health.getCheckedAt() == null) {
                continue;
            }
            LocalDate day = health.getCheckedAt().toLocalDate();
            long[] counts = dayCounts.get(day);
            if (counts == null) {
                continue;
            }
            String status = health.getStatus() != null ? health.getStatus().getStatus() : null;
            if ("UP".equalsIgnoreCase(status)) {
                counts[0]++;
            } else {
                counts[1]++;
            }
        }
        List<AdminStatsSeriesPoint> upPoints = new ArrayList<>();
        List<AdminStatsSeriesPoint> downPoints = new ArrayList<>();
        for (Map.Entry<LocalDate, long[]> entry : dayCounts.entrySet()) {
            String date = entry.getKey().format(DAY_FORMAT);
            upPoints.add(new AdminStatsSeriesPoint(date, entry.getValue()[0]));
            downPoints.add(new AdminStatsSeriesPoint(date, entry.getValue()[1]));
        }
        return buildDailyChart(
                "health",
                "시스템 헬스",
                "건",
                from,
                to,
                List.of(
                        new AdminStatsNamedSeries("up", "정상", upPoints),
                        new AdminStatsNamedSeries("down", "이상", downPoints)));
    }

    private AdminStatsChartResponse buildMetricsChart(
            LocalDate from,
            LocalDate to,
            LocalDateTime fromDateTime,
            LocalDateTime toExclusive) {
        List<Health> history = healthCommandUseCase.getHealthHistoryByDateRange(fromDateTime, toExclusive);
        Map<LocalDate, long[]> heapSumCount = new LinkedHashMap<>();
        Map<LocalDate, long[]> threadSumCount = new LinkedHashMap<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            heapSumCount.put(day, new long[] { 0L, 0L });
            threadSumCount.put(day, new long[] { 0L, 0L });
        }
        for (Health health : history) {
            if (health.getCheckedAt() == null || health.getMetrics() == null) {
                continue;
            }
            LocalDate day = health.getCheckedAt().toLocalDate();
            long[] heap = heapSumCount.get(day);
            long[] thread = threadSumCount.get(day);
            if (heap == null || thread == null) {
                continue;
            }
            long heapPercent = extractHeapUsagePercent(health.getMetrics());
            long threadCount = extractThreadCount(health.getMetrics());
            heap[0] += heapPercent;
            heap[1]++;
            thread[0] += threadCount;
            thread[1]++;
        }
        List<AdminStatsSeriesPoint> heapPoints = new ArrayList<>();
        List<AdminStatsSeriesPoint> threadPoints = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            String date = day.format(DAY_FORMAT);
            long[] heap = heapSumCount.get(day);
            long[] thread = threadSumCount.get(day);
            long heapAvg = heap[1] > 0 ? Math.round((double) heap[0] / heap[1]) : 0L;
            long threadAvg = thread[1] > 0 ? Math.round((double) thread[0] / thread[1]) : 0L;
            heapPoints.add(new AdminStatsSeriesPoint(date, heapAvg));
            threadPoints.add(new AdminStatsSeriesPoint(date, threadAvg));
        }
        return buildDailyChart(
                "metrics",
                "JVM 메트릭",
                "% / 개",
                from,
                to,
                List.of(
                        new AdminStatsNamedSeries("heap", "힙 사용률(%)", heapPoints),
                        new AdminStatsNamedSeries("threads", "스레드 수", threadPoints)));
    }

    private AdminStatsChartResponse buildDailyChart(
            String chartKey,
            String title,
            String unit,
            LocalDate from,
            LocalDate to,
            List<AdminStatsNamedSeries> series) {
        return new AdminStatsChartResponse(chartKey, title, unit, series, from, to);
    }

    private AdminStatsNamedSeries namedDailySeries(
            String name,
            String label,
            LocalDate from,
            LocalDate to,
            Map<LocalDate, Long> counts) {
        List<AdminStatsSeriesPoint> points = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            points.add(new AdminStatsSeriesPoint(day.format(DAY_FORMAT), counts.getOrDefault(day, 0L)));
        }
        return new AdminStatsNamedSeries(name, label, points);
    }

    private Map<String, Object> buildHealthSnapshot() {

        HealthStatus database = healthQueryUseCase.getDatabaseHealth();
        HealthStatus redis = healthQueryUseCase.getRedisHealth();
        Map<String, HealthStatus> external = healthQueryUseCase.getExternalApisHealth();

        HealthStatus overall;
        if ("UP".equals(nullSafeStatus(database)) && "UP".equals(nullSafeStatus(redis))) {
            overall = new HealthStatus("UP", "시스템이 정상입니다",
                    Map.of("database", "UP", "redis", "UP"));
        } else {
            overall = new HealthStatus("DOWN", "일부 구성 요소에 장애가 있습니다",
                    Map.of("database", nullSafeStatus(database), "redis", nullSafeStatus(redis)));
        }

        Map<String, Object> externalSnapshot = external.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Map.of(
                                "status", nullSafeStatus(entry.getValue()),
                                "message", nullSafeMessage(entry.getValue())),
                        (a, b) -> a,
                        LinkedHashMap::new));

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("overall", Map.of(
                "status", nullSafeStatus(overall),
                "message", nullSafeMessage(overall)));
        snapshot.put("database", Map.of(
                "status", nullSafeStatus(database),
                "message", nullSafeMessage(database)));
        snapshot.put("redis", Map.of(
                "status", nullSafeStatus(redis),
                "message", nullSafeMessage(redis)));
        snapshot.put("externalApis", externalSnapshot);
        return snapshot;
    }

    private Map<String, Object> buildMetricsSnapshot() {
        SystemMetrics metrics = healthQueryUseCase.getSystemMetrics();
        Map<String, Object> jvm = metrics.getJvmMetrics();
        Map<String, Object> system = metrics.getSystemMetrics();
        Map<?, ?> memory = asMap(jvm.get("memory"));
        Map<?, ?> threads = asMap(jvm.get("threads"));
        Map<?, ?> os = asMap(system.get("os"));

        long heapUsed = toLong(memory.get("heapUsed"));
        long heapMax = toLong(memory.get("heapMax"));
        long heapUsagePercent = heapMax > 0 ? Math.round(heapUsed * 100.0 / heapMax) : 0L;

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("heapUsedMb", Math.round(heapUsed / (1024.0 * 1024.0)));
        snapshot.put("heapMaxMb", Math.round(heapMax / (1024.0 * 1024.0)));
        snapshot.put("heapUsagePercent", heapUsagePercent);
        snapshot.put("threadCount", toLong(threads.get("count")));
        snapshot.put("processors", toLong(jvm.get("processors")));
        snapshot.put("systemLoadAverage", toDouble(os.get("systemLoadAverage")));
        snapshot.put("cpuUsagePercent", resolveCpuUsagePercent(os));
        snapshot.put("timestamp", metrics.getTimestamp());
        return snapshot;
    }

    private long resolveCpuUsagePercent(Map<?, ?> os) {
        double systemCpuLoad = toDouble(os.get("systemCpuLoad"));
        if (systemCpuLoad >= 0.0d) {
            return Math.round(systemCpuLoad * 100.0d);
        }
        double processCpuLoad = toDouble(os.get("processCpuLoad"));
        if (processCpuLoad >= 0.0d) {
            return Math.round(processCpuLoad * 100.0d);
        }
        return -1L;
    }

    private long extractHeapUsagePercent(SystemMetrics metrics) {
        Map<?, ?> memory = asMap(metrics.getJvmMetrics().get("memory"));
        long heapUsed = toLong(memory.get("heapUsed"));
        long heapMax = toLong(memory.get("heapMax"));
        if (heapMax <= 0) {
            return 0L;
        }
        return Math.round(heapUsed * 100.0 / heapMax);
    }

    private long extractThreadCount(SystemMetrics metrics) {
        Map<?, ?> threads = asMap(metrics.getJvmMetrics().get("threads"));
        return toLong(threads.get("count"));
    }

    private Map<?, ?> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return map;
        }
        return Map.of();
    }

    private int clampDays(Integer days) {
        if (days == null) {
            return DEFAULT_DAYS;
        }
        return Math.max(MIN_DAYS, Math.min(MAX_DAYS, days));
    }

    private Map<LocalDate, Long> toDayCountMap(List<Object[]> rows) {
        Map<LocalDate, Long> map = new HashMap<>();
        if (rows == null) {
            return map;
        }
        for (Object[] row : rows) {
            LocalDate day = toLocalDate(row[0]);
            if (day != null) {
                map.put(day, toLong(row[1]));
            }
        }
        return map;
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof java.util.Date utilDate) {
            return new Date(utilDate.getTime()).toLocalDate();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        String text = value.toString();
        if (text.length() >= 10) {
            return LocalDate.parse(text.substring(0, 10), DAY_FORMAT);
        }
        return LocalDate.parse(text, DAY_FORMAT);
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private double toDouble(Object value) {
        if (value == null) {
            return 0d;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    private String stringify(Object value) {
        return value == null ? null : value.toString();
    }

    private String nullSafeStatus(HealthStatus status) {
        return status == null || status.getStatus() == null ? "UNKNOWN" : status.getStatus();
    }

    private String nullSafeMessage(HealthStatus status) {
        return status == null || status.getMessage() == null ? "" : status.getMessage();
    }
}
