package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.in.NewsCommandUseCase;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsScrapRequesterPort;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
import com.sleekydz86.finsight.core.global.NewsProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class NewsCommandService implements NewsCommandUseCase {

    private final NewsScrapRequesterPort newsScrapRequesterPort;
    private final NewsPersistencePort newsPersistencePort;

    public NewsCommandService(NewsScrapRequesterPort newsScrapRequesterPort,
                              NewsPersistencePort newsPersistencePort) {
        this.newsScrapRequesterPort = newsScrapRequesterPort;
        this.newsPersistencePort = newsPersistencePort;
    }

    @Override
    public Newses scrapNewses() {
        List<NewsProvider> providers = List.of(NewsProvider.MARKETAUX);

        List<News> allNewses = providers.stream()
                .flatMap(provider -> newsScrapRequesterPort.scrap(provider).stream())
                .collect(Collectors.toList());

        return newsPersistencePort.saveAllNews(allNewses);
    }
}