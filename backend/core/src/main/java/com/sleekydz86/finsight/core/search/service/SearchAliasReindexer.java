package com.sleekydz86.finsight.core.search.service;

import com.sleekydz86.finsight.core.global.exception.ValidationException;
import com.sleekydz86.finsight.core.search.adapter.ElasticsearchCatalogGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchAliasReindexer {

    private static final Logger log = LoggerFactory.getLogger(SearchAliasReindexer.class);
    private static final int PAGE = 100;
    private static final int MAX_PAGES = 20;

    private final ObjectProvider<ElasticsearchCatalogGateway> gateway;
    private final MysqlCatalogSearch mysql;

    public SearchAliasReindexer(ObjectProvider<ElasticsearchCatalogGateway> gateway, MysqlCatalogSearch mysql) {
        this.gateway = gateway;
        this.mysql = mysql;
    }

    public SearchReindexReport rebuild() {
        ElasticsearchCatalogGateway search = gateway.getIfAvailable();
        if (search == null) {
            throw new ValidationException("Elasticsearch가 꺼져 있어 재색인할 수 없습니다.", List.of());
        }
        long started = System.nanoTime();
        String from = search.currentConcrete();
        String to = search.nextConcrete(from);
        search.createIndex(to);
        search.copy(from, to);
        int documents = fill(search, to);
        search.refresh(to);
        search.swap(from, to);
        long tookMs = (System.nanoTime() - started) / 1_000_000L;
        log.info("검색 별칭을 교체했습니다. from={} to={} documents={} tookMs={}", from, to, documents, tookMs);
        return new SearchReindexReport(tookMs, documents, from, to);
    }

    private int fill(ElasticsearchCatalogGateway search, String index) {
        int documents = 0;
        documents += fillSource(search, index, "NEWS");
        documents += fillSource(search, index, "VOD");
        documents += fillSource(search, index, "COMMUNITY");
        return documents;
    }

    private int fillSource(ElasticsearchCatalogGateway search, String index, String sourceType) {
        int documents = 0;
        for (int page = 0; page < MAX_PAGES; page++) {
            List<CatalogDocument> batch = mysql.indexPage(sourceType, page, PAGE);
            if (batch.isEmpty()) {
                return documents;
            }
            search.upsert(index, batch);
            documents += batch.size();
            if (batch.size() < PAGE && !"COMMUNITY".equals(sourceType)) {
                return documents;
            }
        }
        return documents;
    }

    public record SearchReindexReport(long tookMs, int documents, String fromIndex, String toIndex) {
    }
}
