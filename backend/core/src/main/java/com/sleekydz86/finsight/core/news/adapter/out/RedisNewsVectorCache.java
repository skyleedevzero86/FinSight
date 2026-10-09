package com.sleekydz86.finsight.core.news.adapter.out;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnBean(name = "customStringRedisTemplate")
public class RedisNewsVectorCache {

    private static final Logger log = LoggerFactory.getLogger(RedisNewsVectorCache.class);
    private static final Duration VECTOR_TTL = Duration.ofDays(7);
    private static final Duration SIMILAR_TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, String> redis;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RedisNewsVectorCache(@Qualifier("customStringRedisTemplate") RedisTemplate<String, String> redis) {
        this.redis = redis;
    }

    public float[] findVector(long newsId) {
        try {
            return readVector(redis.opsForValue().get(vectorKey(newsId)));
        } catch (RuntimeException exception) {
            log.warn("뉴스 임베딩 Redis 조회에 실패했습니다. newsId={}", newsId, exception);
            return null;
        }
    }

    public void saveVector(long newsId, float[] vector) {
        try {
            redis.opsForValue().set(vectorKey(newsId), objectMapper.writeValueAsString(vector), VECTOR_TTL);
        } catch (Exception exception) {
            log.warn("뉴스 임베딩 Redis 저장에 실패했습니다. newsId={}", newsId, exception);
        }
    }

    public List<Long> findSimilar(long newsId) {
        try {
            String raw = redis.opsForValue().get(similarKey(newsId));
            if (raw == null || raw.isBlank()) {
                return List.of();
            }
            List<Long> ids = new ArrayList<>();
            for (String part : raw.split(",")) {
                if (!part.isBlank()) {
                    ids.add(Long.parseLong(part.trim()));
                }
            }
            return ids;
        } catch (RuntimeException exception) {
            log.warn("유사 뉴스 Redis 조회에 실패했습니다. newsId={}", newsId, exception);
            return List.of();
        }
    }

    public void saveSimilar(long newsId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        StringBuilder body = new StringBuilder();
        for (Long id : ids) {
            if (body.length() > 0) {
                body.append(',');
            }
            body.append(id);
        }
        try {
            redis.opsForValue().set(similarKey(newsId), body.toString(), SIMILAR_TTL);
        } catch (RuntimeException exception) {
            log.warn("유사 뉴스 Redis 저장에 실패했습니다. newsId={}", newsId, exception);
        }
    }

    private float[] readVector(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(raw);
            if (!node.isArray() || node.isEmpty()) {
                return null;
            }
            float[] vector = new float[node.size()];
            for (int index = 0; index < node.size(); index++) {
                vector[index] = (float) node.get(index).asDouble();
            }
            return vector;
        } catch (Exception exception) {
            log.warn("뉴스 임베딩 Redis 값을 읽지 못했습니다.", exception);
            return null;
        }
    }

    private String vectorKey(long newsId) {
        return "finsight:news:embedding:" + newsId;
    }

    private String similarKey(long newsId) {
        return "finsight:news:similar:" + newsId;
    }
}
