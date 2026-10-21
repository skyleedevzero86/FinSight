package com.sleekydz86.finsight.core.news.crawl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class NewsCrawlCauseClassifierTest {

    @Test
    @DisplayName("정답 장애셋의 원인 분류가 맞다")
    void classifiesKnownIncidentCauses() {
        // given
        List<Labeled> gold = goldSet();
        int correct = 0;
        for (Labeled labeled : gold) {
            NewsCrawlClassification actual = NewsCrawlCauseClassifier.classify(labeled.error());
            if (actual.cause() == labeled.cause()
                    && java.util.Objects.equals(actual.httpStatus(), labeled.httpStatus())
                    && java.util.Objects.equals(actual.retryAfterSeconds(), labeled.retryAfterSeconds())) {
                correct++;
            }
        }

        // when
        System.out.println("장애 원인 분류 정확도 " + correct + "/" + gold.size());

        // then
        assertThat(correct).isEqualTo(gold.size());
    }

    @Test
    @DisplayName("재처리 가능 여부는 서버 정책으로 정한다")
    void serverPolicyDecidesWhetherRetryIsAllowed() {
        // given
        LocalDateTime occurred = LocalDateTime.of(2026, 10, 15, 9, 0);

        // when

        // then
        assertThat(NewsCrawlRetryPolicy.retryable(NewsCrawlCause.HTTP_401, null, occurred, occurred)).isFalse();
        assertThat(NewsCrawlRetryPolicy.retryable(NewsCrawlCause.HTTP_403, null, occurred, occurred)).isFalse();
        assertThat(NewsCrawlRetryPolicy.retryable(NewsCrawlCause.HTTP_404, null, occurred, occurred)).isFalse();
        assertThat(NewsCrawlRetryPolicy.retryable(NewsCrawlCause.PARSE, null, occurred, occurred)).isFalse();
        assertThat(NewsCrawlRetryPolicy.retryable(NewsCrawlCause.CONNECTION, null, occurred, occurred)).isTrue();
        assertThat(NewsCrawlRetryPolicy.retryable(NewsCrawlCause.TIMEOUT, null, occurred, occurred)).isTrue();
        assertThat(NewsCrawlRetryPolicy.retryable(NewsCrawlCause.HTTP_5XX, null, occurred, occurred)).isTrue();
        assertThat(NewsCrawlRetryPolicy.retryable(NewsCrawlCause.HTTP_429, 30, occurred, occurred.plusSeconds(29))).isFalse();
        assertThat(NewsCrawlRetryPolicy.retryable(NewsCrawlCause.HTTP_429, 30, occurred, occurred.plusSeconds(30))).isTrue();
        assertThat(NewsCrawlRetryPolicy.retryable(NewsCrawlCause.HTTP_429, null, occurred, occurred.plusHours(1))).isFalse();
    }

    private static List<Labeled> goldSet() {
        List<Labeled> gold = new ArrayList<>();
        gold.add(new Labeled(new ConnectException("connection refused"), NewsCrawlCause.CONNECTION, null, null));
        gold.add(new Labeled(new UnknownHostException("news.example"), NewsCrawlCause.CONNECTION, null, null));
        gold.add(new Labeled(new SocketTimeoutException("Read timed out"), NewsCrawlCause.TIMEOUT, null, null));
        gold.add(new Labeled(new TimeoutException("wait"), NewsCrawlCause.TIMEOUT, null, null));
        gold.add(new Labeled(new RuntimeException("wrapped", new ConnectException("refused")), NewsCrawlCause.CONNECTION, null, null));
        gold.add(new Labeled(http(401, null), NewsCrawlCause.HTTP_401, 401, null));
        gold.add(new Labeled(http(403, null), NewsCrawlCause.HTTP_403, 403, null));
        gold.add(new Labeled(http(404, null), NewsCrawlCause.HTTP_404, 404, null));
        gold.add(new Labeled(http(429, "12"), NewsCrawlCause.HTTP_429, 429, 12));
        gold.add(new Labeled(http(500, null), NewsCrawlCause.HTTP_5XX, 500, null));
        gold.add(new Labeled(http(503, null), NewsCrawlCause.HTTP_5XX, 503, null));
        gold.add(new Labeled(parseError(), NewsCrawlCause.PARSE, null, null));
        gold.add(new Labeled(new IllegalStateException("boom"), NewsCrawlCause.UNEXPECTED, null, null));
        return gold;
    }

    private static Throwable parseError() {
        try {
            new ObjectMapper().readTree("{");
        } catch (Exception error) {
            return error;
        }
        throw new IllegalStateException("잘못된 JSON이 파싱되면 안 됩니다");
    }

    private static WebClientResponseException http(int status, String retryAfter) {
        HttpHeaders headers = new HttpHeaders();
        if (retryAfter != null) {
            headers.set(HttpHeaders.RETRY_AFTER, retryAfter);
        }
        return WebClientResponseException.create(status, "status", headers, new byte[0], StandardCharsets.UTF_8);
    }

    private record Labeled(Throwable error, NewsCrawlCause cause, Integer httpStatus, Integer retryAfterSeconds) {
    }
}
