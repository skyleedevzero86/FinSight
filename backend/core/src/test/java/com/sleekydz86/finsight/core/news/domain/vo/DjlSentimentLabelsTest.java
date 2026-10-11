package com.sleekydz86.finsight.core.news.domain.vo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DjlSentimentLabelsTest {

    @Test
    void numericLabelOneIsNeutral() {
        assertThat(DjlSentimentLabels.toType("1")).isEqualTo(SentimentType.NEUTRAL);
        assertThat(DjlSentimentLabels.toType("LABEL_1")).isEqualTo(SentimentType.NEUTRAL);
    }

    @Test
    void numericEndsMatchCardiffLabels() {
        assertThat(DjlSentimentLabels.toType("0")).isEqualTo(SentimentType.NEGATIVE);
        assertThat(DjlSentimentLabels.toType("LABEL_0")).isEqualTo(SentimentType.NEGATIVE);
        assertThat(DjlSentimentLabels.toType("2")).isEqualTo(SentimentType.POSITIVE);
        assertThat(DjlSentimentLabels.toType("label_2")).isEqualTo(SentimentType.POSITIVE);
    }

    @Test
    void wordLabelsIgnoreCase() {
        assertThat(DjlSentimentLabels.toType("positive")).isEqualTo(SentimentType.POSITIVE);
        assertThat(DjlSentimentLabels.toType("Negative")).isEqualTo(SentimentType.NEGATIVE);
        assertThat(DjlSentimentLabels.toType("neutral")).isEqualTo(SentimentType.NEUTRAL);
        assertThat(DjlSentimentLabels.toType(null)).isEqualTo(SentimentType.NEUTRAL);
        assertThat(DjlSentimentLabels.toType(" ")).isEqualTo(SentimentType.NEUTRAL);
    }
}
