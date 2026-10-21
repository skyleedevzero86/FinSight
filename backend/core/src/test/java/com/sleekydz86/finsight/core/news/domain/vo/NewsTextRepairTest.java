package com.sleekydz86.finsight.core.news.domain.vo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class NewsTextRepairTest {

    @Test
    @DisplayName("깨진 S&P 이스케이프를 고친다")
    void repairsBrokenSp500Escape() {
        // given
        String broken = "2026 S\\" + "udc5e0 500 상승";

        // when

        // then
        assertThat(NewsTextRepair.repair(broken)).isEqualTo("2026 S&P 500 상승");
    }

    @Test
    @DisplayName("짝이 없는 서로게이트 이스케이프는 버린다")
    void dropsUnpairedSurrogateEscape() {
        // given
        String broken = "앞\\" + "ud800뒤";

        // when

        // then
        assertThat(NewsTextRepair.repair(broken)).isEqualTo("앞뒤");
    }

    @Test
    @DisplayName("일반 유니코드 이스케이프를 글자로 바꾼다")
    void decodesPlainUnicodeEscape() {
        // given
        String broken = "가\\" + "uac00";

        // when

        // then
        assertThat(NewsTextRepair.repair(broken)).isEqualTo("가가");
    }

    @Test
    @DisplayName("정상 한글은 그대로 둔다")
    void keepsNormalKorean() {
        // given

        // when

        // then
        assertThat(NewsTextRepair.repair("다양한 성장 포트폴리오")).isEqualTo("다양한 성장 포트폴리오");
    }
}
