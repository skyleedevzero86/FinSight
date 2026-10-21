package com.sleekydz86.finsight.core.economypick.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class EconomyPickPanelMathTest {

    @Test
    @DisplayName("보유자산이 있으면 자본과 부채로 나눈다")
    void splitsHoldingsIntoCapitalAndDebt() {
        // given
        EconomyPickAssets ratio = AssetRatioMath.owned(800, 200);

        // when

        // then
        assertThat(ratio.source()).isEqualTo("MINE");
        assertThat(ratio.debtPercent()).isEqualTo(20);
        assertThat(ratio.equityPercent()).isEqualTo(80);
    }

    @Test
    @DisplayName("자산이 없으면 비율을 만들지 않는다")
    void doesNotBuildRatioWithoutAssets() {
        // given

        // when

        // then
        assertThat(AssetRatioMath.owned(0, 0)).isNull();
        assertThat(AssetRatioMath.sample(23).source()).isEqualTo("SAMPLE");
        assertThat(AssetRatioMath.sample(23).equityPercent() + AssetRatioMath.sample(23).debtPercent()).isEqualTo(100);
    }

    @Test
    @DisplayName("버진갤럭틱을 고르면 우주 매출로 추정한다")
    void estimatesSpaceRevenueWhenVirginGalacticIsChosen() {
        // given
        EconomyPickSales mix = SalesMixMath.predict("버진 갤럭틱", "SPCE");

        // when

        // then
        assertThat(mix.note()).contains("버진 갤럭틱");
        assertThat(mix.slices()).extracting(EconomyPickSales.Slice::label).contains("우주관광", "발사 서비스");
        assertThat(mix.slices().stream().mapToDouble(EconomyPickSales.Slice::percent).sum()).isEqualTo(100);
    }

    @Test
    @DisplayName("삼성을 고르면 부문별 매출로 추정한다")
    void estimatesSegmentRevenueWhenSamsungIsChosen() {
        // given
        EconomyPickSales mix = SalesMixMath.predict("삼성전자", "005930");

        // when

        // then
        assertThat(mix.slices()).extracting(EconomyPickSales.Slice::label)
                .contains("TV, 모니터 [DX부문]", "DRAM, NAND [DS부문]");
        assertThat(mix.slices().stream().mapToDouble(EconomyPickSales.Slice::percent).sum()).isEqualTo(100);
    }

    @Test
    @DisplayName("반도체를 고르면 데이터센터 매출이 가장 크다")
    void dataCenterRevenueIsLargestWhenSemiconductorIsChosen() {
        // given
        EconomyPickSales mix = SalesMixMath.predict("엔비디아", "NVDA");

        // when

        // then
        assertThat(mix.slices().get(0).label()).isEqualTo("데이터센터");
        assertThat(mix.slices().get(0).percent()).isGreaterThan(mix.slices().get(1).percent());
    }
}
