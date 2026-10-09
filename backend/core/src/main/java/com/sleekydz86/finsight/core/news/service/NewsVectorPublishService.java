package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.news.adapter.out.RedisNewsVectorCache;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaEntity;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsEmbeddingPort;
import com.sleekydz86.finsight.core.search.adapter.NewsElasticsearchIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;

@Service
public class NewsVectorPublishService {

    private static final Logger log = LoggerFactory.getLogger(NewsVectorPublishService.class);

    private final NewsEmbeddingPort embeddingPort;
    private final ObjectProvider<RedisNewsVectorCache> cache;
    private final ObjectProvider<NewsElasticsearchIndexer> indexer;

    public NewsVectorPublishService(NewsEmbeddingPort embeddingPort,
                                    ObjectProvider<RedisNewsVectorCache> cache,
                                    ObjectProvider<NewsElasticsearchIndexer> indexer) {
        this.embeddingPort = embeddingPort;
        this.cache = cache;
        this.indexer = indexer;
    }

    public boolean enabled() {
        return embeddingPort.isEnabled();
    }

    public boolean ready() {
        return embeddingPort.isReady();
    }

    public String modelName() {
        return embeddingPort.modelName();
    }

    public PublishResult publish(NewsJpaEntity news) {
        float[] vector = cached(news.getId());
        if (vector == null) {
            vector = embeddingPort.embed(textOf(news));
        }
        if (vector == null) {
            return PublishResult.FAILED;
        }
        remember(news.getId(), vector);
        NewsElasticsearchIndexer search = indexer.getIfAvailable();
        if (search == null) {
            log.warn("Elasticsearch가 꺼져 있어 뉴스 임베딩은 Redis에만 둡니다. newsId={}", news.getId());
            return PublishResult.CACHED_ONLY;
        }
        try {
            search.index(news.getId(), titleOf(news), bodyOf(news), publishedAt(news), vector);
            search.refresh();
            return PublishResult.INDEXED;
        } catch (RuntimeException exception) {
            log.warn("Elasticsearch 뉴스 색인에 실패했습니다. newsId={}", news.getId(), exception);
            return PublishResult.CACHED_ONLY;
        }
    }

    private float[] cached(long newsId) {
        RedisNewsVectorCache vectors = cache.getIfAvailable();
        if (vectors == null) {
            return null;
        }
        float[] vector = vectors.findVector(newsId);
        if (vector != null && vector.length != embeddingPort.dimensions()) {
            return null;
        }
        return vector;
    }

    private void remember(long newsId, float[] vector) {
        RedisNewsVectorCache vectors = cache.getIfAvailable();
        if (vectors != null) {
            vectors.saveVector(newsId, vector);
        }
    }

    private String textOf(NewsJpaEntity news) {
        return titleOf(news) + ". " + bodyOf(news);
    }

    private String titleOf(NewsJpaEntity news) {
        if (news.getTranslatedTitle() != null && !news.getTranslatedTitle().isBlank()) {
            return news.getTranslatedTitle();
        }
        return news.getOriginalTitle() == null ? "" : news.getOriginalTitle();
    }

    private String bodyOf(NewsJpaEntity news) {
        if (news.getOverview() != null && !news.getOverview().isBlank()) {
            return news.getOverview();
        }
        return news.getOriginalContent() == null ? "" : news.getOriginalContent();
    }

    private String publishedAt(NewsJpaEntity news) {
        if (news.getNewsPublishedTime() == null) {
            return null;
        }
        return news.getNewsPublishedTime().atOffset(ZoneOffset.UTC).toString();
    }

    public enum PublishResult {
        INDEXED,
        CACHED_ONLY,
        FAILED
    }
}
