package com.sleekydz86.finsight.core.portfolio.service;

import com.sleekydz86.finsight.core.portfolio.domain.PortfolioAssetCategory;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioAssetKind;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioHolding;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioMonthPoint;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioSummary;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioSummaryCalculatorTest {

    private static final YearMonth TODAY = YearMonth.of(2026, 10);

    @Test
    void 자산이_없으면_순자산과_목표는_0이고_등록_안내를_준다() {
        PortfolioSummary summary = PortfolioSummaryCalculator.summarize(List.of(), List.of(), TODAY, false);

        assertThat(summary.netWorth()).isZero();
        assertThat(summary.goalPercentTenths()).isZero();
        assertThat(summary.monthDelta()).isNull();
        assertThat(summary.allocation()).isEmpty();
        assertThat(summary.advice()).isEqualTo("자산을 등록하면 순자산과 구성이 계산됩니다.");
    }

    @Test
    void 총자산에서_부채를_뺀_순자산과_1억_진행률을_계산한다() {
        PortfolioSummary summary = PortfolioSummaryCalculator.summarize(sampleHoldings(), List.of(), TODAY, false);

        assertThat(summary.totalAssets()).isEqualTo(99_520_000L);
        assertThat(summary.totalLiabilities()).isEqualTo(26_000_000L);
        assertThat(summary.netWorth()).isEqualTo(73_520_000L);
        assertThat(summary.goalPercentTenths()).isEqualTo(735);
        assertThat(summary.debtPercentTenths()).isEqualTo(261);
        assertThat(summary.allocation()).extracting(PortfolioSummary.AllocationSlice::percentTenths)
                .containsExactly(100, 270, 350, 180, 100);
    }

    @Test
    void 지난달_기록이_있으면_이번_달_증감을_계산한다() {
        PortfolioMonthPoint previous = new PortfolioMonthPoint(YearMonth.of(2026, 9), 70_900_000L, 26_000_000L);

        PortfolioSummary summary = PortfolioSummaryCalculator.summarize(sampleHoldings(), List.of(previous), TODAY, false);

        assertThat(summary.monthDelta()).isEqualTo(2_620_000L);
        assertThat(summary.yearDelta()).isEqualTo(2_620_000L);
    }

    @Test
    void 올해_이전_기록이_있으면_올해_증감을_계산한다() {
        PortfolioMonthPoint lastYear = new PortfolioMonthPoint(YearMonth.of(2025, 12), 65_210_000L, 28_000_000L);

        PortfolioSummary summary = PortfolioSummaryCalculator.summarize(sampleHoldings(), List.of(lastYear), TODAY, false);

        assertThat(summary.yearDelta()).isEqualTo(8_310_000L);
    }

    @Test
    void 취득가가_있으면_손익률을_계산하고_기록은_최신월이_먼저다() {
        PortfolioHolding coded = new PortfolioHolding(
                1L, "KODEX", PortfolioAssetKind.ASSET, PortfolioAssetCategory.STOCK_ETF, 14_300_000L, null, 11_800_000L);
        PortfolioMonthPoint august = new PortfolioMonthPoint(YearMonth.of(2026, 8), 68_920_000L, 26_000_000L);
        PortfolioMonthPoint september = new PortfolioMonthPoint(YearMonth.of(2026, 9), 70_900_000L, 26_000_000L);

        PortfolioSummary summary = PortfolioSummaryCalculator.summarize(List.of(coded), List.of(august, september), TODAY, true);

        assertThat(summary.assets().get(0).profitRate()).isEqualTo((14_300_000d - 11_800_000d) * 100d / 11_800_000d);
        assertThat(summary.history()).extracting(PortfolioSummary.HistoryRow::yearMonth)
                .containsExactly("2026-09", "2026-08");
        assertThat(summary.history().get(0).delta()).isEqualTo(1_980_000L);
        assertThat(summary.history().get(0).status()).isEqualTo("CONFIRMED");
    }

    @Test
    void 비중이_큰_자산은_경고_문구에_이름을_넣는다() {
        PortfolioSummary summary = PortfolioSummaryCalculator.summarize(sampleHoldings(), List.of(), TODAY, false);

        assertThat(summary.insights()).extracting(PortfolioSummary.InsightLine::text)
                .anyMatch(text -> text.contains("주식·ETF") && text.contains("35%"));
    }

    private static List<PortfolioHolding> sampleHoldings() {
        return List.of(
                holding(1L, "KODEX 미국S&P500", PortfolioAssetCategory.STOCK_ETF, 14_300_000L, 21.2),
                holding(2L, "정기예금", PortfolioAssetCategory.DEPOSIT, 12_000_000L, 3.4),
                holding(3L, "삼성전자", PortfolioAssetCategory.STOCK_ETF, 11_400_000L, 8.1),
                holding(4L, "연금저축", PortfolioAssetCategory.PENSION, 9_800_000L, 6.7),
                holding(5L, "현금성 자산", PortfolioAssetCategory.CASH, 9_950_000L, null),
                holding(6L, "예적금 추가", PortfolioAssetCategory.DEPOSIT, 14_870_000L, null),
                holding(7L, "연금 추가", PortfolioAssetCategory.PENSION, 8_110_000L, null),
                holding(8L, "기타 자산", PortfolioAssetCategory.OTHER, 9_958_000L, null),
                holding(9L, "주식 추가", PortfolioAssetCategory.STOCK_ETF, 9_132_000L, null),
                new PortfolioHolding(10L, "대출", PortfolioAssetKind.LIABILITY, PortfolioAssetCategory.LIABILITY, 26_000_000L, null, null));
    }

    private static PortfolioHolding holding(Long id, String name, PortfolioAssetCategory category, long amount, Double profit) {
        return new PortfolioHolding(id, name, PortfolioAssetKind.ASSET, category, amount, profit, null);
    }
}
