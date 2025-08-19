package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.in.NewsQueryUseCase;
import com.sleekydz86.finsight.core.news.domain.port.in.NewsQueryRequest;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
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
    public Newses findAllByFilters(NewsQueryRequest request) {
        return newsPersistencePort.findAllByFilters(request);
    }
}