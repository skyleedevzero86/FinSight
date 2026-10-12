package com.sleekydz86.finsight.core.search.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.search.config.ElasticsearchProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "finsight.elasticsearch", name = "enabled", havingValue = "true")
public class NewsElasticsearchIndexer {

    private static final Logger log = LoggerFactory.getLogger(NewsElasticsearchIndexer.class);

    private final ElasticsearchProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NewsElasticsearchIndexer(ElasticsearchProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    public void index(long newsId, String title, String body, String publishedAt, float[] embedding) {
        String url = base() + "/" + writeAlias() + "/_doc/news-" + newsId;
        exchange(HttpMethod.PUT, url, document(newsId, title, body, publishedAt, embedding), json());
    }

    public void refresh() {
        exchange(HttpMethod.POST, base() + "/" + readAlias() + "/_refresh", "", json());
    }

    public float[] vectorOf(long newsId) {
        try {
            String body = exchange(HttpMethod.GET, base() + "/" + readAlias() + "/_doc/news-" + newsId, "", json());
            return vector(objectMapper.readTree(body).path("_source").path("embedding"));
        } catch (HttpClientErrorException.NotFound notFound) {
            return null;
        } catch (RestClientException | java.io.IOException exception) {
            log.warn("Elasticsearch 뉴스 임베딩 조회에 실패했습니다. newsId={}", newsId, exception);
            return null;
        }
    }

    public List<Long> similar(float[] vector, long newsId, int limit) {
        int size = Math.max(1, Math.min(limit, 20));
        String body = "{\"size\":" + size
                + ",\"_source\":[\"sourceId\"],\"knn\":{\"field\":\"embedding\",\"query_vector\":"
                + vectorJson(vector)
                + ",\"k\":" + size
                + ",\"num_candidates\":100,\"filter\":{\"bool\":{\"filter\":[{\"term\":{\"sourceType\":\"NEWS\"}}],"
                + "\"must_not\":[{\"term\":{\"sourceId\":\"" + newsId + "\"}}]}}}}";
        String response = exchange(HttpMethod.POST, base() + "/" + readAlias() + "/_search", body, json());
        return ids(response);
    }

    private String document(long newsId, String title, String body, String publishedAt, float[] embedding) {
        try {
            java.util.Map<String, Object> document = new java.util.LinkedHashMap<>();
            document.put("sourceType", "NEWS");
            document.put("sourceId", Long.toString(newsId));
            document.put("title", title == null ? "" : title);
            document.put("body", body == null ? "" : body);
            document.put("href", "/news/" + newsId);
            document.put("publishedAt", publishedAt);
            document.put("viewCount", 0);
            document.put("tags", List.of("news"));
            document.put("embedding", boxed(embedding));
            return objectMapper.writeValueAsString(document);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("뉴스 임베딩 문서를 만들지 못했습니다. newsId=" + newsId, exception);
        }
    }

    private List<Long> ids(String response) {
        try {
            JsonNode hits = objectMapper.readTree(response).path("hits").path("hits");
            List<Long> ids = new ArrayList<>();
            if (!hits.isArray()) {
                return ids;
            }
            for (JsonNode hit : hits) {
                long id = hit.path("_source").path("sourceId").asLong(0);
                if (id > 0) {
                    ids.add(id);
                }
            }
            return ids;
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Elasticsearch 유사 뉴스 응답을 읽지 못했습니다.", exception);
        }
    }

    private float[] vector(JsonNode node) {
        if (!node.isArray() || node.isEmpty()) {
            return null;
        }
        float[] vector = new float[node.size()];
        for (int index = 0; index < node.size(); index++) {
            vector[index] = (float) node.get(index).asDouble();
        }
        return vector;
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

    private String exchange(HttpMethod method, String url, String body, MediaType contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        return restTemplate.exchange(url, method, new HttpEntity<>(body, headers), String.class).getBody();
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
}
