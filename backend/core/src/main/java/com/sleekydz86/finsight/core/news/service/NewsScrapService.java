package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsScrapRequesterPort;
import com.sleekydz86.finsight.core.global.NewsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NewsScrapService {

    private static final Logger log = LoggerFactory.getLogger(NewsScrapService.class);

    private final NewsScrapRequesterPort newsScrapRequesterPort;

    public NewsScrapService(NewsScrapRequesterPort newsScrapRequesterPort) {
        this.newsScrapRequesterPort = newsScrapRequesterPort;
    }

    public List<News> scrapNewsFromProviders() {
        List<NewsProvider> providers = List.of(NewsProvider.MARKETAUX);

        return providers.stream()
                .map(provider -> {
                    try {
                        return newsScrapRequesterPort.scrap(provider);
                    } catch (Exception e) {
                        log.error("뉴스 제공자 {}에서 스크래핑 실패", provider, e);
                        return List.<News>of();
                    }
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }
}