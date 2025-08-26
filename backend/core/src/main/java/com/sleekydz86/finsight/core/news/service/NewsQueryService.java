package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.in.NewsQueryUseCase;
import com.sleekydz86.finsight.core.news.domain.port.in.NewsQueryRequest;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NewsQueryService implements NewsQueryUseCase {

    private final NewsPersistencePort newsPersistencePort;

    public NewsQueryService(NewsPersistencePort newsPersistencePort) {
        this.newsPersistencePort = newsPersistencePort;
    }

    @Override
    @Cacheable(value = "newsCache", key = "#request.hashCode()")
    public Newses findAllByFilters(NewsQueryRequest request) {
        return newsPersistencePort.findAllByFilters(request);
    }

    @Cacheable(value = "newsCache", key = "'all'")
    public Newses findAllNews() {
        return newsPersistencePort.findAllByFilters(new NewsQueryRequest(null, null, null, null, null));
    }
}