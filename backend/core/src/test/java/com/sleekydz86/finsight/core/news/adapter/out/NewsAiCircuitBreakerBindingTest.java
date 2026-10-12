package com.sleekydz86.finsight.core.news.adapter.out;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = NewsAiCircuitBreakerBindingTest.CircuitOnly.class,
        properties = {
                "spring.main.web-application-type=none",
                "spring.config.import=file:./src/main/resources/application.yml"
        })
class NewsAiCircuitBreakerBindingTest {

    @Autowired
    private CircuitBreakerRegistry registry;

    @Test
    void 뉴스_AI_서킷은_5번_호출부터_실패율을_본다() {
        var config = registry.circuitBreaker("newsAiCircuitBreaker").getCircuitBreakerConfig();

        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(5);
        assertThat(config.getSlidingWindowSize()).isEqualTo(10);
    }

    @SpringBootConfiguration
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
