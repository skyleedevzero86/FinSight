package com.sleekydz86.finsight.core.search.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioShareFeed.PortfolioShareGoal;
import com.sleekydz86.finsight.core.portfolio.service.PortfolioTopicPoint;
import com.sleekydz86.finsight.core.portfolio.service.PortfolioTopicRanker;
import com.sleekydz86.finsight.core.portfolio.service.PortfolioTopicVector;
import com.sleekydz86.finsight.core.search.config.ElasticsearchProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "finsight.elasticsearch", name = "enabled", havingValue = "true")
public class PortfolioTopicElasticSearch {

    private final ElasticsearchProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PortfolioTopicElasticSearch(ElasticsearchProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    public List<PortfolioShareGoal> rank(List<PortfolioTopicPoint> points) {
        if (points.isEmpty()) {
            return List.of();
        }
        index(points);
        refresh();
        return PortfolioTopicRanker.rankByNeighbors(points, this::near, PortfolioTopicRanker.LIMIT);
    }

    private List<PortfolioTopicPoint> near(PortfolioTopicPoint seed, List<PortfolioTopicPoint> pending) {
        Map<Long, PortfolioTopicPoint> byId = new HashMap<>();
        for (PortfolioTopicPoint point : pending) {
            byId.put(point.shareId(), point);
        }
        List<PortfolioTopicPoint> near = new ArrayList<>();
        for (Neighbor neighbor : knn(seed.vector())) {
            PortfolioTopicPoint point = byId.get(neighbor.shareId());
            if (point == null) {
                continue;
            }
            double cosine = PortfolioTopicVector.fromElasticsearchScore(neighbor.score());
            if (point.shareId() == seed.shareId()
                    || PortfolioTopicRanker.sameLabel(seed.label(), point.label())
                    || cosine >= PortfolioTopicRanker.SIMILAR) {
                near.add(point);
            }
        }
        return near;
    }

    private void index(List<PortfolioTopicPoint> points) {
        String url = base() + "/" + writeAlias() + "/_bulk";
        StringBuilder body = new StringBuilder();
        for (PortfolioTopicPoint point : points) {
            body.append("{\"index\":{\"_id\":\"portfolio-share-").append(point.shareId()).append("\"}}\n");
            body.append(document(point)).append('\n');
        }
        exchange(HttpMethod.POST, url, body.toString(), ndjson());
    }

    private void refresh() {
        exchange(HttpMethod.POST, base() + "/" + readAlias() + "/_refresh", "", json());
    }

    private List<Neighbor> knn(float[] vector) {
        String body = "{\"size\":100,\"_source\":[\"sourceId\"],\"knn\":{\"field\":\"embedding\",\"query_vector\":"
                + vectorJson(vector)
                + ",\"k\":100,\"num_candidates\":200,\"filter\":{\"term\":{\"sourceType\":\"PORTFOLIO_SHARE\"}}}}";
        String response = exchange(HttpMethod.POST, base() + "/" + readAlias() + "/_search", body, json());
        return neighbors(response);
    }

    private List<Neighbor> neighbors(String response) {
        try {
            JsonNode hits = objectMapper.readTree(response).path("hits").path("hits");
            List<Neighbor> neighbors = new ArrayList<>();
            if (!hits.isArray()) {
                return neighbors;
            }
            for (JsonNode hit : hits) {
                long shareId = hit.path("_source").path("sourceId").asLong(0);
                if (shareId <= 0) {
                    continue;
                }
                neighbors.add(new Neighbor(shareId, hit.path("_score").asDouble(0)));
            }
            return neighbors;
        } catch (Exception exception) {
            throw new IllegalStateException("Elasticsearch 목표 검색 응답을 읽지 못했습니다.", exception);
        }
    }

    private String document(PortfolioTopicPoint point) {
        try {
            Map<String, Object> document = new HashMap<>();
            document.put("sourceType", "PORTFOLIO_SHARE");
            document.put("sourceId", Long.toString(point.shareId()));
            document.put("title", point.label() == null ? "" : point.label());
            document.put("body", point.personKey());
            document.put("href", "/community/free");
            document.put("publishedAt", java.time.Instant.now().toString());
            document.put("viewCount", 0);
            document.put("tags", List.of(point.personKey()));
            document.put("embedding", boxed(point.vector()));
            return objectMapper.writeValueAsString(document);
        } catch (Exception exception) {
            throw new IllegalStateException("포트폴리오 주제 색인 문서를 만들지 못했습니다. shareId=" + point.shareId(), exception);
        }
    }

    private String exchange(HttpMethod method, String url, String body, MediaType contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        try {
            return restTemplate.exchange(url, method, new HttpEntity<>(body, headers), String.class).getBody();
        } catch (RestClientException exception) {
            throw exception;
        }
    }

    private String vectorJson(float[] vector) {
        StringBuilder json = new StringBuilder("[");
        for (int index = 0; index < vector.length; index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append(vector[index]);
        }
        return json.append(']').toString();
    }

    private List<Float> boxed(float[] vector) {
        List<Float> values = new ArrayList<>(vector.length);
        for (float value : vector) {
            values.add(value);
        }
        return values;
    }

    private String base() {
        return ElasticsearchIndexLayout.firstUri(properties.getUris());
    }

    private String readAlias() {
        return properties.getIndex();
    }

    private String writeAlias() {
        return ElasticsearchIndexLayout.writeAlias(properties.getIndex());
    }

    private MediaType json() {
        return MediaType.APPLICATION_JSON;
    }

    private MediaType ndjson() {
        return MediaType.parseMediaType("application/x-ndjson");
    }

    private record Neighbor(long shareId, double score) {
    }
}
