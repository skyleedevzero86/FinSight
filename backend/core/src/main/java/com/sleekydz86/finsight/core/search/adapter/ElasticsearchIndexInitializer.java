package com.sleekydz86.finsight.core.search.adapter;

import com.sleekydz86.finsight.core.search.config.ElasticsearchProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(prefix = "finsight.elasticsearch", name = "enabled", havingValue = "true")
public class ElasticsearchIndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchIndexInitializer.class);

    private final ElasticsearchProperties properties;
    private final RestTemplate restTemplate;

    public ElasticsearchIndexInitializer(ElasticsearchProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureSearchAlias();
        } catch (RestClientException | IllegalArgumentException exception) {
            log.warn("Elasticsearch 검색 색인 준비를 건너뜁니다. uri={} index={}",
                    properties.getUris(), properties.getIndex(), exception);
        }
    }

    private void ensureSearchAlias() {
        String base = ElasticsearchIndexLayout.firstUri(properties.getUris());
        String readAlias = properties.getIndex() == null ? "" : properties.getIndex().trim();
        if (aliasExists(base, readAlias)) {
            log.info("Elasticsearch 검색 별칭이 이미 있습니다. alias={}", readAlias);
            return;
        }
        if (concreteIndexExists(base, readAlias)) {
            log.warn("Elasticsearch 인덱스가 별칭이 아닙니다. 무중단 재색인을 쓰려면 기존 인덱스를 지운 뒤 다시 준비하세요. index={}",
                    readAlias);
            return;
        }
        createAliasedIndex(base, readAlias);
    }

    private void createAliasedIndex(String base, String readAlias) {
        String concrete = ElasticsearchIndexLayout.initialConcrete(readAlias);
        String writeAlias = ElasticsearchIndexLayout.writeAlias(readAlias);
        if (!concreteIndexExists(base, concrete)) {
            exchangeJson(HttpMethod.PUT, base + "/" + concrete,
                    ElasticsearchIndexLayout.indexBody(properties.getVectorDims()));
        }
        exchangeJson(HttpMethod.POST, base + "/_aliases",
                ElasticsearchIndexLayout.aliasBody(readAlias, writeAlias, concrete));
        log.info("Elasticsearch 한글·벡터 색인을 만들었습니다. index={} readAlias={} writeAlias={} dims={}",
                concrete, readAlias, writeAlias, properties.getVectorDims());
    }

    private void exchangeJson(HttpMethod method, String url, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.exchange(url, method, new HttpEntity<>(body, headers), String.class);
    }

    private boolean aliasExists(String base, String readAlias) {
        return exists(base + "/_alias/" + readAlias);
    }

    private boolean concreteIndexExists(String base, String name) {
        return exists(base + "/" + name);
    }

    private boolean exists(String url) {
        try {
            restTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, String.class);
            return true;
        } catch (HttpClientErrorException.NotFound notFound) {
            return false;
        }
    }
}
