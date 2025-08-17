package com.sleekydz86.batch.news.scrap.job;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaEntity;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaRepository;
import com.sleekydz86.finsight.core.news.adapter.requester.overview.properties.NewsOpenAiAnalysisRequester;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.adapter.requester.NewsScrapRequester;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.dto.AiChatRequest;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.dto.AiChatResponse;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.vo.NewsMeta;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBatchTest
@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
public class NewsScrapJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @MockitoBean
    private NewsScrapRequester newsScrapRequester;

    @MockitoBean
    private NewsOpenAiAnalysisRequester newsOpenAiAnalysisRequester;

    @MockitoBean
    private NewsJpaRepository newsJpaRepository;

    @Test
    public void 뉴스를_크롤링하고_ai_분석을_정상적으로_진행한다() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addString("publishTimeAfter", "2025-08-14T00:00:00")
                .addLong("limit", 5L)
                .addLong("runTime", System.currentTimeMillis())
                .toJobParameters();

        List<News> newsList = Arrays.asList(
                News.createWithoutAI(
                        new NewsMeta(
                                NewsProvider.MARKETAUX,
                                LocalDateTime.now(),
                                "https://example.com"
                        ),
                        new Content("title", "content")
                )
        );

        AiChatResponse aiResponse = new AiChatResponse(
                Arrays.asList(
                        new AiChatResponse.NewsAnalysis(
                                "ai overview",
                                "ai translated title",
                                "ai translated content",
                                Arrays.asList(TargetCategory.BTC),
                                SentimentType.POSITIVE,
                                1.0
                        )
                )
        );

        NewsJpaEntity mockEntity = new NewsJpaEntity();
        mockEntity.setOverview("ai overview");
        List<NewsJpaEntity> mockEntities = Arrays.asList(mockEntity);

        when(newsScrapRequester.supports()).thenReturn(NewsProvider.MARKETAUX);
        when(newsScrapRequester.scrap(any(LocalDateTime.class), any(Integer.class)))
                .thenReturn(CompletableFuture.completedFuture(newsList));
        when(newsOpenAiAnalysisRequester.request(any(AiChatRequest.class)))
                .thenReturn(aiResponse);
        when(newsJpaRepository.findAll()).thenReturn(mockEntities);

        JobExecution result = jobLauncherTestUtils.launchJob(params);

        List<NewsJpaEntity> foundNewses = newsJpaRepository.findAll();

        assertAll(
                () -> assertEquals("COMPLETED", result.getExitStatus().getExitCode()),
                () -> assertEquals(1, foundNewses.size()),
                () -> assertEquals("ai overview", foundNewses.get(0).getOverview())
        );
    }
}