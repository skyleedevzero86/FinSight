package com.sleekydz86.finsight.core.search.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.search.adapter.ElasticsearchCatalogGateway;
import com.sleekydz86.finsight.core.search.adapter.RedisSearchCache;
import com.sleekydz86.finsight.core.search.domain.CatalogSearchResult;
import com.sleekydz86.finsight.core.search.domain.CatalogSearchResult.SearchSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

@Service
public class CatalogSearchService {

    private static final Logger log = LoggerFactory.getLogger(CatalogSearchService.class);
    private static final long SLOW_MS = 200;

    private final MysqlCatalogSearch mysql;
    private final ObjectProvider<ElasticsearchCatalogGateway> elasticsearch;
    private final RedisSearchCache redis;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CatalogSearchService(
            MysqlCatalogSearch mysql,
            ObjectProvider<ElasticsearchCatalogGateway> elasticsearch,
            RedisSearchCache redis) {
        this.mysql = mysql;
        this.elasticsearch = elasticsearch;
        this.redis = redis;
    }

    public CatalogSearchResult search(String rawQuery, Long userId, String email) {
        long started = System.nanoTime();
        String keyword = SearchSnippets.keyword(rawQuery);
        if (keyword.isBlank()) {
            return new CatalogSearchResult("", 0, 0, "MYSQL", false, List.of());
        }
        String cacheKey = "search:public:" + keyword.toLowerCase();
        List<SearchSection> publicSections = cached(cacheKey);
        boolean cached = publicSections != null;
        String engine = cached ? "REDIS" : "MYSQL";
        if (!cached) {
            PublicSlice slice = publicSections(keyword);
            publicSections = slice.sections();
            engine = slice.engine();
            String cachedJson = write(publicSections);
            if (!cachedJson.isBlank()) {
                redis.write(cacheKey, cachedJson);
            }
        }
        List<SearchSection> sections = new ArrayList<>(publicSections);
        sections.add(mysql.mine(keyword, userId, email));
        long tookMs = (System.nanoTime() - started) / 1_000_000L;
        redis.record(tookMs, engine);
        if (tookMs >= SLOW_MS) {
            log.warn("검색이 느립니다. queryLength={} tookMs={} engine={}", keyword.length(), tookMs, engine);
        }
        long total = sections.stream().mapToLong(SearchSection::total).sum();
        return new CatalogSearchResult(keyword, total, tookMs, engine, cached, sections);
    }

    private PublicSlice publicSections(String keyword) {
        ElasticsearchCatalogGateway search = elasticsearch.getIfAvailable();
        if (search == null) {
            return stored(keyword);
        }
        try {
            return indexed(search, keyword);
        } catch (RestClientException | IllegalStateException exception) {
            log.warn("Elasticsearch 검색에 실패해 MySQL로 조회합니다. queryLength={}", keyword.length(), exception);
            return stored(keyword);
        }
    }

    private PublicSlice indexed(ElasticsearchCatalogGateway search, String keyword) {
        SearchSection news = search.search(List.of("NEWS"), "뉴스", "news", keyword);
        SearchSection videos = search.search(List.of("VOD"), "실시간 VOD", "vod", keyword);
        SearchSection community = search.search(
                List.of("COMMUNITY", "NOTICE", "QNA", "FREE"), "커뮤니티", "community", keyword);
        boolean usedIndex = news.total() > 0 || videos.total() > 0 || community.total() > 0;
        if (news.total() == 0) {
            news = mysql.news(keyword);
        }
        if (videos.total() == 0) {
            videos = mysql.videos(keyword);
        }
        if (community.total() == 0) {
            community = mysql.community(keyword);
        }
        return new PublicSlice(usedIndex ? "ELASTICSEARCH" : "MYSQL", List.of(news, videos, community));
    }

    private PublicSlice stored(String keyword) {
        return new PublicSlice("MYSQL", List.of(mysql.news(keyword), mysql.videos(keyword), mysql.community(keyword)));
    }

    private List<SearchSection> cached(String key) {
        return redis.read(key).map(this::read).orElse(null);
    }

    private List<SearchSection> read(String json) {
        try {
            return objectMapper.readerForListOf(SearchSection.class).readValue(json);
        } catch (java.io.IOException exception) {
            log.warn("Redis 검색 캐시 형식이 올바르지 않습니다.", exception);
            return null;
        }
    }

    private String write(List<SearchSection> sections) {
        try {
            return objectMapper.writeValueAsString(sections);
        } catch (java.io.IOException exception) {
            log.warn("검색 결과를 캐시 문자열로 만들지 못했습니다.", exception);
            return "";
        }
    }

    private record PublicSlice(String engine, List<SearchSection> sections) {
    }
}
