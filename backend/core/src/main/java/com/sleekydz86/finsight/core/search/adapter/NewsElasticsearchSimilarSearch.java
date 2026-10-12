package com.sleekydz86.finsight.core.search.adapter;

import com.sleekydz86.finsight.core.news.adapter.out.RedisNewsVectorCache;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsSimilarSearchPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "finsight.elasticsearch", name = "enabled", havingValue = "true")
public class NewsElasticsearchSimilarSearch implements NewsSimilarSearchPort {

    private final NewsElasticsearchIndexer indexer;
    private final ObjectProvider<RedisNewsVectorCache> cache;

    public NewsElasticsearchSimilarSearch(NewsElasticsearchIndexer indexer,
                                          ObjectProvider<RedisNewsVectorCache> cache) {
        this.indexer = indexer;
        this.cache = cache;
    }

    @Override
    public List<Long> findSimilarIds(long newsId, int limit) {
        int size = Math.max(1, Math.min(limit, 20));
        RedisNewsVectorCache vectors = cache.getIfAvailable();
        List<Long> cached = vectors == null ? List.of() : vectors.findSimilar(newsId);
        if (!cached.isEmpty()) {
            return trim(cached, size);
        }
        float[] vector = vectors == null ? null : vectors.findVector(newsId);
        if (vector == null) {
            vector = indexer.vectorOf(newsId);
        }
        if (vector == null) {
            return List.of();
        }
        List<Long> ids = indexer.similar(vector, newsId, size);
        if (vectors != null) {
            vectors.saveSimilar(newsId, ids);
        }
        return ids;
    }

    private List<Long> trim(List<Long> ids, int limit) {
        if (ids.size() <= limit) {
            return ids;
        }
        return new ArrayList<>(ids.subList(0, limit));
    }
}
