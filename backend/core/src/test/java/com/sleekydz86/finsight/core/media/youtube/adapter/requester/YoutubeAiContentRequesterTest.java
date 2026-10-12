package com.sleekydz86.finsight.core.media.youtube.adapter.requester;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.board.domain.Board;
import com.sleekydz86.finsight.core.board.domain.BoardStatus;
import com.sleekydz86.finsight.core.board.domain.BoardType;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeGeneratedContent;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeVideoMeta;
import com.sleekydz86.finsight.core.news.adapter.requester.overview.properties.OllamaProperties;
import com.sleekydz86.finsight.core.news.adapter.requester.overview.properties.OpenAiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class YoutubeAiContentRequesterTest {

    @Test
    void 설명문_없이_요약과_핵심포인트를_만든다() {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.just(ClientResponseFactory.ok(generatedJson())))
                .build();
        OpenAiProperties openAiProperties = new OpenAiProperties();
        openAiProperties.setApiKey("test-key");
        openAiProperties.setModel("gpt-4o-mini");
        OllamaProperties ollamaProperties = new OllamaProperties();
        ollamaProperties.setEnabled(false);
        YoutubeAiContentRequester requester = new YoutubeAiContentRequester(
                webClient,
                webClient,
                openAiProperties,
                ollamaProperties,
                new ObjectMapper(),
                "openai");
        YoutubeVideoMeta meta = meta();
        assertThat(requester.buildPrompt(meta, board())).doesNotContain("비트겟");
        YoutubeGeneratedContent generated = requester.generate(meta, board());

        assertThat(generated.getSummary()).contains("금리");
        assertThat(generated.getEditorComment()).contains("금리");
        assertThat(generated.getKeyPoints()).containsExactly("국채 금리", "순환매", "제목 범위");
    }

    private static YoutubeVideoMeta meta() {
        return YoutubeVideoMeta.builder()
                .boardId(1L)
                .youtubeTitle("10년물 국채 금리")
                .youtubeDescription("광고 구독 비트겟 가입 링크")
                .channelTitle("삼프로TV")
                .category("THEME")
                .build();
    }

    @Test
    void 로컬_프로필은_올라마로_보강한다() {
        AtomicReference<String> requestedUrl = new AtomicReference<>();
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> {
                    requestedUrl.set(request.url().toString());
                    return Mono.just(ClientResponseFactory.ok(ollamaJson()));
                })
                .build();
        OpenAiProperties openAiProperties = new OpenAiProperties();
        openAiProperties.setApiKey("test-key");
        OllamaProperties ollamaProperties = new OllamaProperties();
        ollamaProperties.setEnabled(true);
        ollamaProperties.setBaseUrl("http://127.0.0.1:11434");
        ollamaProperties.setModel("llama3.2");
        YoutubeAiContentRequester requester = new YoutubeAiContentRequester(
                webClient,
                webClient,
                openAiProperties,
                ollamaProperties,
                new ObjectMapper(),
                "ollama");

        YoutubeGeneratedContent generated = requester.generate(meta(), board());

        assertThat(requestedUrl.get()).isEqualTo("http://127.0.0.1:11434/api/chat");
        assertThat(generated.getSummary()).contains("금리");
        assertThat(generated.getKeyPoints()).hasSize(3);
    }

    private static Board board() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 7, 13, 0);
        return new Board(
                1L,
                "10년물 국채 금리",
                "광고 본문",
                "media-batch@finsight.local",
                BoardType.MEDIA,
                BoardStatus.ACTIVE,
                0,
                0,
                0,
                0,
                0,
                List.of(),
                List.of(),
                now,
                now,
                List.of(),
                false);
    }

    private static String ollamaJson() {
        return """
                {"message":{"content":"{\\"summary\\":\\"국채 금리 흐름을 정리한 영상입니다.\\",\\"editorComment\\":\\"제목에 나온 금리만 짚습니다.\\",\\"keyPoints\\":[\\"국채 금리\\",\\"순환매\\",\\"제목 범위\\"]}"}}
                """;
    }

    private static String generatedJson() {
        return """
                {"choices":[{"message":{"content":"{\\"summary\\":\\"국채 금리 흐름을 정리한 영상입니다. 순환매를 함께 다룹니다.\\",\\"editorComment\\":\\"제목에 나온 금리와 순환매만 짚습니다. 설명문의 수치는 사용하지 않습니다.\\",\\"keyPoints\\":[\\"국채 금리\\",\\"순환매\\",\\"제목 범위\\"]}"}}]}
                """;
    }

    private static final class ClientResponseFactory {
        private ClientResponseFactory() {
        }

        private static org.springframework.web.reactive.function.client.ClientResponse ok(String body) {
            return org.springframework.web.reactive.function.client.ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body)
                    .build();
        }
    }
}
