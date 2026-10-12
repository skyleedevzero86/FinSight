package com.sleekydz86.finsight.core.news.domain.vo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NewsTextRepairTest {

    @Test
    void repairsBrokenSp500Escape() {
        String broken = "2026 S\\" + "udc5e0 500 상승";
        assertThat(NewsTextRepair.repair(broken)).isEqualTo("2026 S&P 500 상승");
    }

    @Test
    void dropsUnpairedSurrogateEscape() {
        String broken = "앞\\" + "ud800뒤";
        assertThat(NewsTextRepair.repair(broken)).isEqualTo("앞뒤");
    }

    @Test
    void decodesPlainUnicodeEscape() {
        String broken = "가\\" + "uac00";
        assertThat(NewsTextRepair.repair(broken)).isEqualTo("가가");
    }

    @Test
    void keepsNormalKorean() {
        assertThat(NewsTextRepair.repair("다양한 성장 포트폴리오")).isEqualTo("다양한 성장 포트폴리오");
    }
}
