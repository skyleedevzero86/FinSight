package com.sleekydz86.finsight.core.portfolio.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioShareSignalsTest {

    @Test
    void 신고가_있으면_블라인드다() {
        assertThat(PortfolioShareSignals.display("OPEN", 0, 1)).isEqualTo("BLIND");
    }

    @Test
    void 싫어요가_있으면_경고다() {
        assertThat(PortfolioShareSignals.display("OPEN", 1, 0)).isEqualTo("WARN");
        assertThat(PortfolioShareSignals.normal("WARN", 10)).isFalse();
    }

    @Test
    void 좋아요_10개_이상이면_정상이다() {
        assertThat(PortfolioShareSignals.display("OPEN", 0, 0)).isEqualTo("OPEN");
        assertThat(PortfolioShareSignals.normal("OPEN", 10)).isTrue();
        assertThat(PortfolioShareSignals.normal("OPEN", 9)).isFalse();
    }
}
