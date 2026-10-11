package com.sleekydz86.finsight.core.media.youtube.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class YoutubeVideoMetaAiStatusTest {

    @Test
    void 보강_시각이_없으면_대기다() {
        YoutubeVideoMeta meta = YoutubeVideoMeta.builder().build();

        assertThat(meta.resolveAiStatus()).isEqualTo("PENDING");
    }

    @Test
    void 실패_시각만_있으면_실패다() {
        YoutubeVideoMeta meta = YoutubeVideoMeta.builder()
                .aiFailedAt(LocalDateTime.of(2026, 10, 7, 1, 0))
                .build();

        assertThat(meta.resolveAiStatus()).isEqualTo("FAILED");
    }

    @Test
    void 보강_시각이_있으면_실패_시각보다_완료가_우선이다() {
        YoutubeVideoMeta meta = YoutubeVideoMeta.builder()
                .aiGeneratedAt(LocalDateTime.of(2026, 10, 7, 2, 0))
                .aiFailedAt(LocalDateTime.of(2026, 10, 7, 1, 0))
                .build();

        assertThat(meta.resolveAiStatus()).isEqualTo("DONE");
    }
}
