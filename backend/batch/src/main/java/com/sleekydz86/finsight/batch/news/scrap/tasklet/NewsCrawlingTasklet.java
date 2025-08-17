package com.sleekydz86.finsight.batch.news.scrap.tasklet;

import com.sleekydz86.finsight.core.news.adapter.requester.NewsScrapRequester;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
@StepScope
public class NewsCrawlingTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(NewsCrawlingTasklet.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final Set<NewsScrapRequester> newsScrapRequesters;
    private final NewsPersistencePort newsPersistencePort;

    public NewsCrawlingTasklet(Set<NewsScrapRequester> newsScrapRequesters,
                               NewsPersistencePort newsPersistencePort) {
        this.newsScrapRequesters = newsScrapRequesters;
        this.newsPersistencePort = newsPersistencePort;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        try {
            JobParameters jobParameters = contribution.getStepExecution().getJobParameters();
            String publishTimeAfterParam = jobParameters.getString("publishTimeAfter");
            Long limitLong = jobParameters.getLong("limit");
            int limitParam = limitLong != null ? limitLong.intValue() : 3;

            LocalDateTime publishTimeAfter = parsePublishTimeAfter(publishTimeAfterParam);
            List<News> newses = scrapAll(publishTimeAfter, limitParam);
            newsPersistencePort.saveAllNews(newses);

            log.info("총 {}개의 뉴스 저장 완료 (기준 시간: {}, limit: {})",
                    newses.size(), publishTimeAfter, limitParam);
            return RepeatStatus.FINISHED;
        } catch (Exception e) {
            log.error("뉴스 수집 중 오류 발생", e);
            throw e;
        }
    }

    private List<News> scrapAll(LocalDateTime publishTimeAfter, int limit) {
        List<CompletableFuture<List<News>>> futures = new ArrayList<>();

        for (NewsScrapRequester requester : newsScrapRequesters) {
            String providerName = requester.supports().name();

            log.info("[{}] 뉴스 수집 시작 - 기준 시간: {}, limit: {}",
                    providerName, publishTimeAfter, limit);

            CompletableFuture<List<News>> future = requester.scrap(publishTimeAfter, limit)
                    .handle((newses, throwable) -> {
                        if (throwable != null) {
                            log.error("[{}] 뉴스 수집 실패", providerName, throwable);
                            return Collections.<News>emptyList();
                        } else {
                            log.info("[{}] {}개의 뉴스 수집 완료", providerName, newses.size());
                            return newses;
                        }
                    });

            futures.add(future);
        }

        return futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private LocalDateTime parsePublishTimeAfter(String publishTimeAfterParam) {
        try {
            return LocalDateTime.parse(publishTimeAfterParam, DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("잘못된 publishTimeAfter 파라미터 형식: {}. 기본값(1일 전)으로 설정합니다.",
                    publishTimeAfterParam);
            return LocalDateTime.now().minusDays(1);
        }
    }
}
