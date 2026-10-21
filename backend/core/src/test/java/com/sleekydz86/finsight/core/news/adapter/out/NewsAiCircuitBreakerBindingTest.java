package com.sleekydz86.finsight.core.news.adapter.out;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import com.sleekydz86.finsight.core.global.config.NewsAiCircuitBreakerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = NewsAiCircuitBreakerBindingTest.CircuitOnly.class,
        properties = "spring.main.web-application-type=none")
class NewsAiCircuitBreakerBindingTest {

    @Autowired
    private CircuitBreakerRegistry registry;

    @Test
    @DisplayName("뉴스 AI 서킷은 5번 호출부터 실패율을 본다")
    void newsAiCircuitReadsFailureRateFromTheFifthCall() {
        // given

        // when
        var config = registry.circuitBreaker("newsAiCircuitBreaker").getCircuitBreakerConfig();

        // then
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(5);
        assertThat(config.getSlidingWindowSize()).isEqualTo(10);
    }

    @SpringBootConfiguration
    @Import(NewsAiCircuitBreakerConfig.class)
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            RedisAutoConfiguration.class,
            SecurityAutoConfiguration.class,
            ManagementWebSecurityAutoConfiguration.class,
            MailSenderAutoConfiguration.class,
            BatchAutoConfiguration.class
    })
    static class CircuitOnly {
    }
}
