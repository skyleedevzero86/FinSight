package com.sleekydz86.finsight.core.news.domain.vo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DjlSentimentScoreTest {

    @Test
    void directionIsPositiveMinusNegativeAndConfidenceIsMax() {
        DjlSentimentScore score = DjlSentimentScore.from(List.of(
                new DjlClassProbability("negative", 0.1),
                new DjlClassProbability("neutral", 0.2),
                new DjlClassProbability("positive", 0.7)));

        assertThat(score.type()).isEqualTo(SentimentType.POSITIVE);
        assertThat(score.direction()).isEqualTo(0.7 - 0.1);
        assertThat(score.confidence()).isEqualTo(0.7);
        assertThat(score.positive()).isEqualTo(0.7);
        assertThat(score.neutral()).isEqualTo(0.2);
        assertThat(score.negative()).isEqualTo(0.1);
    }

    @Test
    void labelOneWithHighestProbabilityStaysNeutral() {
        DjlSentimentScore score = DjlSentimentScore.from(List.of(
                new DjlClassProbability("0", 0.2),
                new DjlClassProbability("1", 0.7),
                new DjlClassProbability("2", 0.1)));

        assertThat(score.type()).isEqualTo(SentimentType.NEUTRAL);
        assertThat(score.direction()).isEqualTo(0.1 - 0.2);
        assertThat(score.confidence()).isEqualTo(0.7);
    }
}
