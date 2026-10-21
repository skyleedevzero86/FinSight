package com.sleekydz86.finsight.core.news.crawl;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

public final class NewsCrawlCauseClassifier {

    private NewsCrawlCauseClassifier() {
    }

    public static NewsCrawlClassification classify(Throwable error) {
        WebClientResponseException http = null;
        boolean connection = false;
        boolean timeout = false;
        boolean parse = false;
        Throwable current = error;
        int guard = 0;
        while (current != null && guard++ < 12) {
            if (current instanceof WebClientResponseException response) {
                http = response;
            }
            if (current instanceof ConnectException || current instanceof UnknownHostException) {
                connection = true;
            }
            if (isTimeout(current)) {
                timeout = true;
            }
            if (current instanceof JsonProcessingException) {
                parse = true;
            }
            current = current.getCause();
        }
        if (http != null) {
            return fromHttp(http);
        }
        if (connection) {
            return new NewsCrawlClassification(NewsCrawlCause.CONNECTION, null, null);
        }
        if (timeout) {
            return new NewsCrawlClassification(NewsCrawlCause.TIMEOUT, null, null);
        }
        if (parse) {
            return new NewsCrawlClassification(NewsCrawlCause.PARSE, null, null);
        }
        return new NewsCrawlClassification(NewsCrawlCause.UNEXPECTED, null, null);
    }

    private static boolean isTimeout(Throwable error) {
        if (error instanceof TimeoutException || error instanceof SocketTimeoutException) {
            return true;
        }
        String name = error.getClass().getName();
        return name.endsWith("TimeoutException") || name.endsWith("ReadTimeoutException");
    }

    private static NewsCrawlClassification fromHttp(WebClientResponseException http) {
        int status = http.getStatusCode().value();
        Integer retryAfter = status == 429 ? retryAfterSeconds(http) : null;
        NewsCrawlCause cause = switch (status) {
            case 401 -> NewsCrawlCause.HTTP_401;
            case 403 -> NewsCrawlCause.HTTP_403;
            case 404 -> NewsCrawlCause.HTTP_404;
            case 429 -> NewsCrawlCause.HTTP_429;
            default -> status >= 500 && status <= 599 ? NewsCrawlCause.HTTP_5XX : NewsCrawlCause.UNEXPECTED;
        };
        return new NewsCrawlClassification(cause, status, retryAfter);
    }

    private static Integer retryAfterSeconds(WebClientResponseException http) {
        String header = http.getHeaders().getFirst(HttpHeaders.RETRY_AFTER);
        if (header == null || header.isBlank()) {
            return null;
        }
        String digits = header.trim();
        if (!digits.matches("\\d{1,9}")) {
            return null;
        }
        return Integer.parseInt(digits);
    }
}
