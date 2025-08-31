package com.sleekydz86.batch.news.scrap.job;

import com.sleekydz86.finsight.batch.helper.annotations.JobIntegrationTest;
import com.sleekydz86.finsight.batch.news.scrap.job.NewsScrapJobConfig;
import com.sleekydz86.finsight.core.news.adapter.requester.NewsScrapRequester;
import com.sleekydz86.finsight.core.news.adapter.requester.overview.properties.NewsOpenAiAnalysisRequester;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@JobIntegrationTest(jobClasses = {NewsScrapJobConfig.class, NewsScrapJobTest.TestConfig.class})
@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@SpringBatchTest
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class NewsScrapJobTest {

        @Autowired
        private JobLauncherTestUtils jobLauncherTestUtils;

        @TestConfiguration
        static class TestConfig {
                @Bean
                @Primary
                public NewsScrapRequester newsScrapRequester() {
                        return mock(NewsScrapRequester.class);
                }

                @Bean
                @Primary
                public NewsOpenAiAnalysisRequester newsOpenAiAnalysisRequester() {
                        return mock(NewsOpenAiAnalysisRequester.class);
                }

                @Bean
                @Primary
                public NewsJpaRepository newsJpaRepository() {
                        return mock(NewsJpaRepository.class);
                }
        }

        @Test
        public void 뉴스를_크롤링하고_ai_분석을_정상적으로_진행한다() throws Exception {
                // given

                // when
                JobExecution jobExecution = jobLauncherTestUtils.launchJob();

                // then
                assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        }
}