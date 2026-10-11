package com.sleekydz86.finsight.core.portfolio.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioShareTextTest {

    @Test
    void 이름은_첫_글자만_남긴다() {
        assertThat(PortfolioShareText.maskName("studio_kim")).isEqualTo("s*********");
        assertThat(PortfolioShareText.maskName("김민수")).isEqualTo("김**");
        assertThat(PortfolioShareText.maskName("A")).isEqualTo("A");
    }

    @Test
    void 금액_구간은_천만_원_단위로_내린다() {
        assertThat(PortfolioShareText.amountLabel("BAND", 73_520_000L)).isEqualTo("약 7,000만원");
        assertThat(PortfolioShareText.amountLabel("EXACT", 20_000_000L)).isEqualTo("20,000,000원");
        assertThat(PortfolioShareText.amountLabel("RATIO", 20_000_000L)).isEqualTo("비율만");
    }

    @Test
    void 목표_진행률은_100을_넘기지_않는다() {
        assertThat(PortfolioShareText.progress(735)).isEqualTo(73);
        assertThat(PortfolioShareText.progress(1200)).isEqualTo(100);
    }

    @Test
    void 이번_달_증감률을_부호와_함께_만든다() {
        assertThat(PortfolioShareText.signedPercent(2_620_000L, 73_520_000L)).isEqualTo("+3.7%");
    }
}
