package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.news.adapter.requester.scrap.properties.YahooFinanceNewsScrapRequester;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Profile("local")
public class LocalYahooNewsWarmup {

    private static final Logger log = LoggerFactory.getLogger(LocalYahooNewsWarmup.class);

    private final YahooFinanceNewsScrapRequester yahooFinanceNewsScrapRequester;
    private final NewsPersistencePort newsPersistencePort;

    public LocalYahooNewsWarmup(YahooFinanceNewsScrapRequester yahooFinanceNewsScrapRequester,
                                NewsPersistencePort newsPersistencePort) {
        this.yahooFinanceNewsScrapRequester = yahooFinanceNewsScrapRequester;
        this.newsPersistencePort = newsPersistencePort;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void collect() {
        try {
            List<News> rows = yahooFinanceNewsScrapRequester
                    .scrap(LocalDateTime.now().minusDays(3), 12)
                    .join();
            if (rows.isEmpty()) {
                log.warn("Yahoo Finance 신규 뉴스가 없습니다.");
                return;
            }
            newsPersistencePort.saveAllNews(rows);
            log.info("Yahoo Finance 뉴스 {}건을 저장했습니다.", rows.size());
        } catch (Exception exception) {
            log.warn("Yahoo Finance 뉴스 수집 실패: {}", exception.getMessage());
        }
    }
}
