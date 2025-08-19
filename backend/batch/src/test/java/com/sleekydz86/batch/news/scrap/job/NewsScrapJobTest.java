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
                // Given
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
                                                                "https://example.com"),
                                                new Content("SoftBank makes surprise $2 billion bet on Intel's AI revival",
                                                                "SoftBank buys $2 billion of Intel stock, boosting US chipmaker and its own AI ambitions.")));

                AiChatResponse aiResponse = new AiChatResponse(
                                Arrays.asList(
                                                new AiChatResponse.NewsAnalysis(
                                                                "소프트뱅크가 인텔에 20억 달러 투자하여 AI 부활을 지원",
                                                                "소프트뱅크, 인텔의 AI 부활에 20억 달러 투자",
                                                                "소프트뱅크가 인텔 주식에 20억 달러를 투자하여 미국 반도체 업체와 자체 AI 야망을 부양시킵니다.",
                                                                Arrays.asList(TargetCategory.NVDA, TargetCategory.MSFT),
                                                                SentimentType.POSITIVE,
                                                                0.8)));

                NewsJpaEntity mockEntity = new NewsJpaEntity();
                mockEntity.setOverview("소프트뱅크가 인텔에 20억 달러 투자하여 AI 부활을 지원");
                List<NewsJpaEntity> mockEntities = Arrays.asList(mockEntity);

                // When
                when(newsScrapRequester.supports()).thenReturn(NewsProvider.MARKETAUX);
                when(newsScrapRequester.scrap(any(LocalDateTime.class), any(Integer.class)))
                                .thenReturn(CompletableFuture.completedFuture(newsList));
                when(newsOpenAiAnalysisRequester.request(any(AiChatRequest.class)))
                                .thenReturn(aiResponse);
                when(newsJpaRepository.findAll()).thenReturn(mockEntities);

                JobExecution result = jobLauncherTestUtils.launchJob(params);

                // Then
                List<NewsJpaEntity> foundNewses = newsJpaRepository.findAll();

                assertAll(
                                () -> assertEquals("COMPLETED", result.getExitStatus().getExitCode()),
                                () -> assertEquals(1, foundNewses.size()),
                                () -> assertEquals("소프트뱅크가 인텔에 20억 달러 투자하여 AI 부활을 지원",
                                                foundNewses.get(0).getOverview()));
        }
}