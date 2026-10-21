package com.sleekydz86.finsight.core.news.adapter.requester.overview.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OllamaAnalysisRequesterTest {

    @Test
    @DisplayName("객체로 온 Ollama JSON도 읽는다")
    void readsObjectPayload() {
        // given
        String payload = "{\"translatedTitle\":\"에너지 문제가 커진 AI\",\"sentimentType\":\"NEGATIVE\"}";

        // when
        String json = OllamaAnalysisRequester.jsonPayload(payload);

        // then
        assertThat(json).startsWith("{");
        assertThat(json).contains("에너지 문제가 커진 AI");
    }

    @Test
    @DisplayName("배열과 코드펜스도 JSON으로 걷어낸다")
    void readsArrayInsideFence() {
        // given
        String payload = "```json\n[{\"translatedTitle\":\"금리 유지\"}]\n```";

        // when
        String json = OllamaAnalysisRequester.jsonPayload(payload);

        // then
        assertThat(json).startsWith("[");
        assertThat(json).contains("금리 유지");
    }
}
