package com.sleekydz86.finsight.core.news.adapter.out;

import com.sleekydz86.finsight.core.news.domain.port.out.NewsScrapRequesterPort;
import com.sleekydz86.finsight.core.news.adapter.requester.NewsScrapRequester;
import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.News;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
public class NewsScrapRequesterAdapter implements NewsScrapRequesterPort {

    private final Set<NewsScrapRequester> newsScrapRequesters;

    public NewsScrapRequesterAdapter(Set<NewsScrapRequester> newsScrapRequesters) {
        this.newsScrapRequesters = newsScrapRequesters;
    }

    @Override
    public List<News> scrap(NewsProvider newsProvider) {
        return newsScrapRequesters.stream()
                .filter(requester -> requester.supports() == newsProvider)
                .findFirst()
                .map(requester -> {
                    try {
                        return requester.scrap(LocalDateTime.now().minusHours(1), 10)
                                .get(); // CompletableFuture를 동기적으로 처리
                    } catch (Exception e) {
                        throw new RuntimeException("뉴스 스크래핑 중 오류 발생", e);
                    }
                })
                .orElse(List.of());
    }
}