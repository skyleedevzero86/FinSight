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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@JobIntegrationTest(jobClasses = {NewsScrapJobConfig.class})
@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@ActiveProfiles("test")
public class NewsScrapJobTest {

        @Autowired
        private JobLauncherTestUtils jobLauncherTestUtils;

        @MockBean
        private NewsScrapRequester newsScrapRequester;

        @MockBean
        private NewsOpenAiAnalysisRequester newsOpenAiAnalysisRequester;

        @MockBean
        private NewsJpaRepository newsJpaRepository;

        @Test
        public void 뉴스를_크롤링하고_ai_분석을_정상적으로_진행한다() throws Exception {
                // when
                JobExecution jobExecution = jobLauncherTestUtils.launchJob();

                // then
                assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        }
}