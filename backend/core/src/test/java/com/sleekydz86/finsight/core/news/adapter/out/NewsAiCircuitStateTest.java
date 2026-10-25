package com.sleekydz86.finsight.core.news.adapter.out;

import com.sleekydz86.finsight.core.global.AiModel;
import com.sleekydz86.finsight.core.global.config.NewsAiCircuitBreakerConfig;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.NewsAiRequester;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.dto.AiChatRequest;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.dto.AiChatResponse;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.news.service.AiModelSelectionService;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        classes = NewsAiCircuitStateTest.CircuitOnly.class,
        properties = {
                "spring.main.web-application-type=none",
                "finsight.news-ai.circuit-breaker.wait-duration-in-open-state=200ms",
                "finsight.news-ai.circuit-breaker.slow-call-duration-threshold=50ms"
        })
class NewsAiCircuitStateTest {

    @Autowired
    private NewsAiAnalysisRequesterAdapter adapter;

    @Autowired
    private ScriptedNewsAiRequester requester;

    @Autowired
    private CircuitBreakerRegistry registry;

    private CircuitBreaker breaker;
    private final Content content = new Content("제목", "본문");

    @BeforeEach
    void setUp() {
        breaker = registry.circuitBreaker("newsAiCircuitBreaker");
        breaker.reset();
        requester.reset();
    }

    @Test
    @DisplayName("빈 AI 응답 5번이면 서킷이 열린다")
    void emptyAiResponseCountsAsFailure() {
        // given
        requester.returnEmpty();

        // when
        for (int attempt = 0; attempt < 5; attempt++) {
            assertThat(adapter.analyseNewses(AiModel.CHATGPT, content)).isEmpty();
        }

        // then
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(adapter.analyseNewses(AiModel.CHATGPT, content)).isEmpty();
        assertThat(requester.calls()).isEqualTo(5);
    }

    @Test
    @DisplayName("느린 AI 응답은 실패로 집계되어 서킷이 열린다")
    void slowAiCallCountsAsFailure() {
        // given
        requester.returnSlowSuccess(80);

        // when
        for (int attempt = 0; attempt < 5; attempt++) {
            assertThat(adapter.analyseNewses(AiModel.CHATGPT, content)).isNotEmpty();
        }

        // then
        assertThat(breaker.getMetrics().getNumberOfSlowCalls()).isEqualTo(5);
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    @DisplayName("열린 서킷은 대기 뒤 시험 호출 2번이 성공하면 닫힌다")
    void circuitClosesAfterHalfOpenSuccess() throws InterruptedException {
        // given
        requester.fail();
        for (int attempt = 0; attempt < 5; attempt++) {
            assertThatThrownBy(() -> adapter.analyseNewses(AiModel.CHATGPT, content))
                    .isInstanceOf(IllegalStateException.class);
        }
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        Thread.sleep(openWaitMillis() + 200);
        requester.returnSuccess();

        // when
        List<News> firstTrial = adapter.analyseNewses(AiModel.CHATGPT, content);
        CircuitBreaker.State afterFirstTrial = breaker.getState();
        List<News> secondTrial = adapter.analyseNewses(AiModel.CHATGPT, content);

        // then
        assertThat(firstTrial).isNotEmpty();
        assertThat(afterFirstTrial).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        assertThat(secondTrial).isNotEmpty();
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("서킷이 열려 fallback이 호출되면 건너뜀 경고만 남긴다")
    void openCircuitLogsSkip() {
        // given
        requester.returnEmpty();
        for (int attempt = 0; attempt < 5; attempt++) {
            adapter.analyseNewses(AiModel.CHATGPT, content);
        }
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // when
        List<ILoggingEvent> events = capture(() -> adapter.analyseNewses(AiModel.CHATGPT, content));

        // then
        assertThat(events)
                .filteredOn(event -> event.getLevel() == Level.WARN)
                .anyMatch(event -> event.getFormattedMessage().contains("서킷이 열려"));
        assertThat(events)
                .filteredOn(event -> event.getFormattedMessage().contains("모든 모델이 실패"))
                .isEmpty();
        assertThat(requester.calls()).isEqualTo(5);
    }

    @Test
    @DisplayName("모델 실패로 fallback이 호출되면 오류 로그를 남긴다")
    void modelFailureLogsErrorInsteadOfOpenCircuit() {
        // given
        requester.fail();

        // when
        List<ILoggingEvent> events = capture(() -> assertThatThrownBy(
                () -> adapter.analyseNewses(AiModel.CHATGPT, content))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CHATGPT"));

        // then
        assertThat(events)
                .filteredOn(event -> event.getLevel() == Level.ERROR)
                .anyMatch(event -> event.getFormattedMessage().contains("모든 모델이 실패")
                        && event.getFormattedMessage().contains("AI 분석에 실패했습니다"));
        assertThat(events)
                .filteredOn(event -> event.getFormattedMessage().contains("서킷이 열려"))
                .isEmpty();
    }

    private List<ILoggingEvent> capture(Runnable action) {
        Logger logger = (Logger) LoggerFactory.getLogger(NewsAiAnalysisRequesterAdapter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            action.run();
        } finally {
            logger.detachAppender(appender);
        }
        return List.copyOf(appender.list);
    }

    private long openWaitMillis() {
        return breaker.getCircuitBreakerConfig().getWaitIntervalFunctionInOpenState().apply(1);
    }

    @SpringBootConfiguration
    @Import({
            NewsAiCircuitBreakerConfig.class,
            NewsAiAnalysisRequesterAdapter.class,
            AiModelSelectionService.class,
            ScriptedNewsAiRequester.class
    })
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

    static class ScriptedNewsAiRequester implements NewsAiRequester {
        private final AtomicInteger calls = new AtomicInteger();
        private long delayMillis;
        private AiChatResponse response;
        private RuntimeException failure;

        void reset() {
            calls.set(0);
            delayMillis = 0;
            response = null;
            failure = null;
        }

        void returnEmpty() {
            failure = null;
            delayMillis = 0;
            response = new AiChatResponse(List.of());
        }

        void returnSuccess() {
            failure = null;
            delayMillis = 0;
            response = analysed();
        }

        void returnSlowSuccess(long delayMillis) {
            failure = null;
            this.delayMillis = delayMillis;
            response = analysed();
        }

        void fail() {
            response = null;
            delayMillis = 0;
            failure = new IllegalStateException("모델 장애");
        }

        int calls() {
            return calls.get();
        }

        @Override
        public AiModel supports() {
            return AiModel.CHATGPT;
        }

        @Override
        public AiChatResponse request(AiChatRequest aiChatRequest) throws InterruptedException {
            calls.incrementAndGet();
            if (delayMillis > 0) {
                Thread.sleep(delayMillis);
            }
            if (failure != null) {
                throw failure;
            }
            return response;
        }

        private static AiChatResponse analysed() {
            return new AiChatResponse(List.of(new AiChatResponse.NewsAnalysis(
                    "요약",
                    "번역 제목",
                    "번역 본문",
                    List.of(TargetCategory.NONE),
                    SentimentType.NEUTRAL,
                    0.5)));
        }
    }
}
