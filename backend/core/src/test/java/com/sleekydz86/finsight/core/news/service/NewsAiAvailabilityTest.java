package com.sleekydz86.finsight.core.news.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class NewsAiAvailabilityTest {

    @Test
    @DisplayName("OpenAI 키가 있으면 연결 가능으로 본다")
    void treatsConnectionAsAvailableWhenOpenAiKeyExists() {
        // given

        // when

        // then
        assertThat(NewsAiAvailability.decide(true, false, false)).isTrue();
    }

    @Test
    @DisplayName("키가 없고 Ollama가 꺼져 있으면 관리자 안내가 필요하다")
    void asksForAdminWhenKeyIsMissingAndOllamaIsOff() {
        // given

        // when

        // then
        assertThat(NewsAiAvailability.decide(false, false, true)).isFalse();
    }

    @Test
    @DisplayName("키가 없어도 Ollama가 연결되면 사용할 수 있다")
    void isUsableWhenOllamaIsConnectedWithoutAKey() {
        // given

        // when

        // then
        assertThat(NewsAiAvailability.decide(false, true, true)).isTrue();
    }

    @Test
    @DisplayName("Ollama가 켜져 있어도 연결이 없으면 사용할 수 없다")
    void isUnusableWhenOllamaIsEnabledButUnreachable() {
        // given

        // when

        // then
        assertThat(NewsAiAvailability.decide(false, true, false)).isFalse();
    }
}
