package com.sleekydz86.finsight.core.search.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.search.config.ElasticsearchProperties;
import com.sleekydz86.finsight.core.search.domain.CatalogSearchResult.SearchHit;
import com.sleekydz86.finsight.core.search.domain.CatalogSearchResult.SearchSection;
import com.sleekydz86.finsight.core.search.service.CatalogDocument;
import com.sleekydz86.finsight.core.search.service.SearchSnippets;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "finsight.elasticsearch", name = "enabled", havingValue = "true")
public class ElasticsearchCatalogGateway {

    private static final int HITS = 5;

    private final ElasticsearchProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ElasticsearchCatalogGateway(ElasticsearchProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    public SearchSection search(List<String> sourceTypes, String label, String key, String keyword) {
        String body = query(sourceTypes, keyword);
        String response = exchange(HttpMethod.POST, base() + "/" + readAlias() + "/_search", body);
        return parse(key, label, keyword, response);
    }

    public String nextConcrete(String current) {
        return ElasticsearchIndexLayout.nextConcrete(current);
    }

    public String currentConcrete() {
        String response = exchange(HttpMethod.GET, base() + "/_alias/" + readAlias(), "");
        try {
            Iterator<String> names = objectMapper.readTree(response).fieldNames();
            if (!names.hasNext()) {
                throw new IllegalStateException("읽기 별칭에 물리 인덱스가 없습니다. alias=" + readAlias());
            }
            return names.next();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("읽기 별칭을 읽지 못했습니다. alias=" + readAlias(), exception);
        }
    }

    public void createIndex(String concrete) {
        exchange(HttpMethod.PUT, base() + "/" + concrete, ElasticsearchIndexLayout.indexBody(properties.getVectorDims()));
    }

    public void copy(String fromIndex, String toIndex) {
        String body = "{\"source\":{\"index\":\"" + fromIndex + "\"},\"dest\":{\"index\":\"" + toIndex + "\"}}";
        exchange(HttpMethod.POST, base() + "/_reindex?wait_for_completion=true", body);
    }

    public void upsert(String index, List<CatalogDocument> documents) {
        if (documents.isEmpty()) {
            return;
        }
        StringBuilder bulk = new StringBuilder();
        for (CatalogDocument document : documents) {
            bulk.append(updateLine(index, document)).append('\n');
            bulk.append(docLine(document)).append('\n');
        }
        exchange(HttpMethod.POST, base() + "/_bulk", bulk.toString(), MediaType.parseMediaType("application/x-ndjson"));
    }

    public void refresh(String index) {
        exchange(HttpMethod.POST, base() + "/" + index + "/_refresh", "");
    }

    public void swap(String fromIndex, String toIndex) {
        exchange(HttpMethod.POST, base() + "/_aliases",
                ElasticsearchIndexLayout.swapAliasBody(readAlias(), writeAlias(), fromIndex, toIndex));
    }

    private SearchSection parse(String key, String label, String keyword, String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            long total = root.path("hits").path("total").path("value").asLong(0);
            List<SearchHit> hits = new ArrayList<>();
            for (JsonNode hit : root.path("hits").path("hits")) {
                JsonNode source = hit.path("_source");
                String title = SearchSnippets.title(source.path("title").asText(""), "");
                String snippet = highlight(hit, keyword, source.path("body").asText(""));
                hits.add(new SearchHit(title, snippet, source.path("href").asText("")));
            }
            return new SearchSection(key, label, total, "", hits);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Elasticsearch 검색 응답을 읽지 못했습니다.", exception);
        }
    }

    private String highlight(JsonNode hit, String keyword, String body) {
        JsonNode fragments = hit.path("highlight").path("body");
        if (fragments.isArray() && !fragments.isEmpty()) {
            return SearchSnippets.snippet(fragments.get(0).asText(""), "", keyword);
        }
        return SearchSnippets.snippet(body, hit.path("_source").path("title").asText(""), keyword);
    }

    private String query(List<String> sourceTypes, String keyword) {
        Map<String, Object> match = new LinkedHashMap<>();
        match.put("query", keyword);
        match.put("fields", List.of("title^3", "body"));
        match.put("type", "best_fields");
        match.put("operator", "and");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("size", HITS);
        body.put("track_total_hits", true);
        body.put("_source", List.of("title", "body", "href"));
        body.put("query", Map.of("bool", Map.of(
                "filter", List.of(Map.of("terms", Map.of("sourceType", sourceTypes))),
                "must", List.of(Map.of("multi_match", match)))));
        body.put("highlight", Map.of(
                "pre_tags", List.of(""),
                "post_tags", List.of(""),
                "fields", Map.of("body", Map.of("fragment_size", 120, "number_of_fragments", 1))));
        try {
            return objectMapper.writeValueAsString(body);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Elasticsearch 검색 요청을 만들지 못했습니다.", exception);
        }
    }

    private String updateLine(String index, CatalogDocument document) {
        return "{\"update\":{\"_index\":\"" + index + "\",\"_id\":\"" + document.sourceType().toLowerCase()
                + "-" + document.sourceId() + "\"}}";
    }

    private String docLine(CatalogDocument document) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("sourceType", document.sourceType());
        fields.put("sourceId", document.sourceId());
        fields.put("title", document.title());
        fields.put("body", clip(document.body()));
        fields.put("href", document.href());
        fields.put("viewCount", 0);
        fields.put("tags", List.of(document.sourceType().toLowerCase()));
        try {
            return objectMapper.writeValueAsString(Map.of("doc", fields, "doc_as_upsert", true));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("검색 문서를 직렬화하지 못했습니다. id=" + document.sourceId(), exception);
        }
    }

    private String clip(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= 4000 ? body : body.substring(0, 4000);
    }

    private String exchange(HttpMethod method, String url, String body) {
        return exchange(method, url, body, MediaType.APPLICATION_JSON);
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
}
