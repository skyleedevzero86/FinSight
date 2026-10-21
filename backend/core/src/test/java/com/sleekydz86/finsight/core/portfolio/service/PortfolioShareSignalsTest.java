package com.sleekydz86.finsight.core.portfolio.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioShareSignalsTest {

    @Test
    @DisplayName("신고가 있으면 블라인드다")
    void reportMakesCardBlind() {
        // given

        // when

        // then
        assertThat(PortfolioShareSignals.display("OPEN", 0, 1)).isEqualTo("BLIND");
    }

    @Test
    @DisplayName("싫어요가 있으면 경고다")
    void dislikeMakesCardWarning() {
        // given

        // when

        // then
        assertThat(PortfolioShareSignals.display("OPEN", 1, 0)).isEqualTo("WARN");
        assertThat(PortfolioShareSignals.normal("WARN", 10)).isFalse();
    }

    @Test
    @DisplayName("좋아요 10개 이상이면 정상이다")
    void tenOrMoreLikesKeepCardOpen() {
        // given

        // when

        // then
        assertThat(PortfolioShareSignals.display("OPEN", 0, 0)).isEqualTo("OPEN");
        assertThat(PortfolioShareSignals.normal("OPEN", 10)).isTrue();
        assertThat(PortfolioShareSignals.normal("OPEN", 9)).isFalse();
    }
}
