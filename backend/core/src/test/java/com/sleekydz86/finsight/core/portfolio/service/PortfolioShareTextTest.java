package com.sleekydz86.finsight.core.portfolio.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioShareTextTest {

    @Test
    @DisplayName("이름은 첫 글자만 남긴다")
    void keepsOnlyTheFirstLetterOfTheName() {
        // given

        // when

        // then
        assertThat(PortfolioShareText.maskName("studio_kim")).isEqualTo("s*********");
        assertThat(PortfolioShareText.maskName("김민수")).isEqualTo("김**");
        assertThat(PortfolioShareText.maskName("A")).isEqualTo("A");
    }

    @Test
    @DisplayName("금액 구간은 천만 원 단위로 내린다")
    void roundsAmountBandDownToTenMillionWon() {
        // given

        // when

        // then
        assertThat(PortfolioShareText.amountLabel("BAND", 73_520_000L)).isEqualTo("약 7,000만원");
        assertThat(PortfolioShareText.amountLabel("EXACT", 20_000_000L)).isEqualTo("20,000,000원");
        assertThat(PortfolioShareText.amountLabel("RATIO", 20_000_000L)).isEqualTo("비율만");
    }

    @Test
    @DisplayName("목표 진행률은 100을 넘기지 않는다")
    void goalProgressDoesNotExceedOneHundred() {
        // given

        // when

        // then
        assertThat(PortfolioShareText.progress(735)).isEqualTo(73);
        assertThat(PortfolioShareText.progress(1200)).isEqualTo(100);
    }

    @Test
    @DisplayName("이번 달 증감률을 부호와 함께 만든다")
    void buildsThisMonthChangeRateWithSign() {
        // given

        // when

        // then
        assertThat(PortfolioShareText.signedPercent(2_620_000L, 73_520_000L)).isEqualTo("+3.7%");
    }
}
