package com.sleekydz86.finsight.core.media.youtube.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class YoutubeVideoMetaAiStatusTest {

    @Test
    @DisplayName("보강 시각이 없으면 대기다")
    void waitsWhenEnrichmentTimeIsMissing() {
        // given
        YoutubeVideoMeta meta = YoutubeVideoMeta.builder().build();

        // when

        // then
        assertThat(meta.resolveAiStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("실패 시각만 있으면 실패다")
    void isFailedWhenOnlyFailureTimeExists() {
        // given
        YoutubeVideoMeta meta = YoutubeVideoMeta.builder()
                .aiFailedAt(LocalDateTime.of(2026, 10, 7, 1, 0))
                .build();

        // when

        // then
        assertThat(meta.resolveAiStatus()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("보강 시각이 있으면 실패 시각보다 완료가 우선이다")
    void completionBeatsFailureTimeWhenEnrichmentTimeExists() {
        // given
        YoutubeVideoMeta meta = YoutubeVideoMeta.builder()
                .aiGeneratedAt(LocalDateTime.of(2026, 10, 7, 2, 0))
                .aiFailedAt(LocalDateTime.of(2026, 10, 7, 1, 0))
                .build();

        // when

        // then
        assertThat(meta.resolveAiStatus()).isEqualTo("DONE");
    }
}
