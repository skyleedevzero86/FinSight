package com.sleekydz86.finsight.core.economypick.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EconomyPickPanelMathTest {

    @Test
    void 보유자산이_있으면_자본과_부채로_나눈다() {
        EconomyPickAssets ratio = AssetRatioMath.owned(800, 200);
        assertThat(ratio.source()).isEqualTo("MINE");
        assertThat(ratio.debtPercent()).isEqualTo(20);
        assertThat(ratio.equityPercent()).isEqualTo(80);
    }

    @Test
    void 자산이_없으면_비율을_만들지_않는다() {
        assertThat(AssetRatioMath.owned(0, 0)).isNull();
        assertThat(AssetRatioMath.sample(23).source()).isEqualTo("SAMPLE");
        assertThat(AssetRatioMath.sample(23).equityPercent() + AssetRatioMath.sample(23).debtPercent()).isEqualTo(100);
    }

    @Test
    void 버진갤럭틱을_고르면_우주_매출로_추정한다() {
        EconomyPickSales mix = SalesMixMath.predict("버진 갤럭틱", "SPCE");
        assertThat(mix.note()).contains("버진 갤럭틱");
        assertThat(mix.slices()).extracting(EconomyPickSales.Slice::label).contains("우주관광", "발사 서비스");
        assertThat(mix.slices().stream().mapToDouble(EconomyPickSales.Slice::percent).sum()).isEqualTo(100);
    }

    @Test
    void 삼성을_고르면_부문별_매출로_추정한다() {
        EconomyPickSales mix = SalesMixMath.predict("삼성전자", "005930");
        assertThat(mix.slices()).extracting(EconomyPickSales.Slice::label)
                .contains("TV, 모니터 [DX부문]", "DRAM, NAND [DS부문]");
        assertThat(mix.slices().stream().mapToDouble(EconomyPickSales.Slice::percent).sum()).isEqualTo(100);
    }

    @Test
    void 반도체를_고르면_데이터센터_매출이_가장_크다() {
        EconomyPickSales mix = SalesMixMath.predict("엔비디아", "NVDA");
        assertThat(mix.slices().get(0).label()).isEqualTo("데이터센터");
        assertThat(mix.slices().get(0).percent()).isGreaterThan(mix.slices().get(1).percent());
    }
}
