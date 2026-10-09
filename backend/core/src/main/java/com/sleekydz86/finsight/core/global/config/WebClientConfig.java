package com.sleekydz86.finsight.core.global.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    private static final long TIMEOUT_SECOND = 30;
    private static final int CONNECTION_TIMEOUT_TIME_MILLIS = 10000;

    @Bean
    @Primary
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(TIMEOUT_SECOND))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECTION_TIMEOUT_TIME_MILLIS);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean(name = "ollamaWebClient")
    public WebClient ollamaWebClient(@Value("${ai.ollama.timeout-seconds:180}") long timeoutSeconds) {
        long seconds = Math.max(timeoutSeconds, 30);
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(seconds))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECTION_TIMEOUT_TIME_MILLIS);
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(CONNECTION_TIMEOUT_TIME_MILLIS))
                .setReadTimeout(Duration.ofSeconds(TIMEOUT_SECOND))
                .build();
    }
}