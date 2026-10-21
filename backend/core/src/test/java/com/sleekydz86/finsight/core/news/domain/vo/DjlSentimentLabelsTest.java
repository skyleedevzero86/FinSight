package com.sleekydz86.finsight.core.news.domain.vo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class DjlSentimentLabelsTest {

    @Test
    @DisplayName("숫자 라벨 1은 중립이다")
    void numericLabelOneIsNeutral() {
        // given

        // when

        // then
        assertThat(DjlSentimentLabels.toType("1")).isEqualTo(SentimentType.NEUTRAL);
        assertThat(DjlSentimentLabels.toType("LABEL_1")).isEqualTo(SentimentType.NEUTRAL);
    }

    @Test
    @DisplayName("숫자 양 끝은 Cardiff 라벨과 같다")
    void numericEndsMatchCardiffLabels() {
        // given

        // when

        // then
        assertThat(DjlSentimentLabels.toType("0")).isEqualTo(SentimentType.NEGATIVE);
        assertThat(DjlSentimentLabels.toType("LABEL_0")).isEqualTo(SentimentType.NEGATIVE);
        assertThat(DjlSentimentLabels.toType("2")).isEqualTo(SentimentType.POSITIVE);
        assertThat(DjlSentimentLabels.toType("label_2")).isEqualTo(SentimentType.POSITIVE);
    }

    @Test
    @DisplayName("단어 라벨은 대소문자를 구분하지 않는다")
    void wordLabelsIgnoreCase() {
        // given

        // when

        // then
        assertThat(DjlSentimentLabels.toType("positive")).isEqualTo(SentimentType.POSITIVE);
        assertThat(DjlSentimentLabels.toType("Negative")).isEqualTo(SentimentType.NEGATIVE);
        assertThat(DjlSentimentLabels.toType("neutral")).isEqualTo(SentimentType.NEUTRAL);
        assertThat(DjlSentimentLabels.toType(null)).isEqualTo(SentimentType.NEUTRAL);
        assertThat(DjlSentimentLabels.toType(" ")).isEqualTo(SentimentType.NEUTRAL);
    }
}
