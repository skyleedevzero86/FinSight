package com.sleekydz86.finsight.core.portfolio.service;

import com.sleekydz86.finsight.core.portfolio.domain.PortfolioAssetCategory;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioAssetKind;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioHolding;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioMonthPoint;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioSummaryCalculatorTest {

    private static final YearMonth TODAY = YearMonth.of(2026, 10);

    @Test
    @DisplayName("자산이 없으면 순자산과 목표는 0이고 등록 안내를 준다")
    void zeroNetWorthAndGoalWithRegistrationGuideWhenNoAsset() {
        // given
        PortfolioSummary summary = PortfolioSummaryCalculator.summarize(List.of(), List.of(), TODAY, false);

        // when

        // then
        assertThat(summary.netWorth()).isZero();
        assertThat(summary.goalPercentTenths()).isZero();
        assertThat(summary.monthDelta()).isNull();
        assertThat(summary.allocation()).isEmpty();
        assertThat(summary.advice()).isEqualTo("자산을 등록하면 순자산과 구성이 계산됩니다.");
    }

    @Test
    @DisplayName("총자산에서 부채를 뺀 순자산과 1억 진행률을 계산한다")
    void calculatesNetWorthAfterDebtAndProgressTowardOneHundredMillion() {
        // given
        PortfolioSummary summary = PortfolioSummaryCalculator.summarize(sampleHoldings(), List.of(), TODAY, false);

        // when

        // then
        assertThat(summary.totalAssets()).isEqualTo(99_520_000L);
        assertThat(summary.totalLiabilities()).isEqualTo(26_000_000L);
        assertThat(summary.netWorth()).isEqualTo(73_520_000L);
        assertThat(summary.goalPercentTenths()).isEqualTo(735);
        assertThat(summary.debtPercentTenths()).isEqualTo(261);
        assertThat(summary.allocation()).extracting(PortfolioSummary.AllocationSlice::percentTenths)
                .containsExactly(100, 270, 350, 180, 100);
    }

    @Test
    @DisplayName("지난달 기록이 있으면 이번 달 증감을 계산한다")
    void calculatesThisMonthChangeWhenLastMonthExists() {
        // given
        PortfolioMonthPoint previous = new PortfolioMonthPoint(YearMonth.of(2026, 9), 70_900_000L, 26_000_000L);
        PortfolioSummary summary = PortfolioSummaryCalculator.summarize(sampleHoldings(), List.of(previous), TODAY, false);

        // when

        // then
        assertThat(summary.monthDelta()).isEqualTo(2_620_000L);
        assertThat(summary.yearDelta()).isEqualTo(2_620_000L);
    }

    @Test
    @DisplayName("올해 이전 기록이 있으면 올해 증감을 계산한다")
    void calculatesThisYearChangeWhenEarlierRecordExists() {
        // given
        PortfolioMonthPoint lastYear = new PortfolioMonthPoint(YearMonth.of(2025, 12), 65_210_000L, 28_000_000L);
        PortfolioSummary summary = PortfolioSummaryCalculator.summarize(sampleHoldings(), List.of(lastYear), TODAY, false);

        // when

        // then
        assertThat(summary.yearDelta()).isEqualTo(8_310_000L);
    }

    @Test
    @DisplayName("취득가가 있으면 손익률을 계산하고 기록은 최신월이 먼저다")
    void calculatesProfitRateFromCostAndListsLatestMonthFirst() {
        // given
        PortfolioHolding coded = new PortfolioHolding(
                1L, "KODEX", PortfolioAssetKind.ASSET, PortfolioAssetCategory.STOCK_ETF, 14_300_000L, null, 11_800_000L);
        PortfolioMonthPoint august = new PortfolioMonthPoint(YearMonth.of(2026, 8), 68_920_000L, 26_000_000L);
        PortfolioMonthPoint september = new PortfolioMonthPoint(YearMonth.of(2026, 9), 70_900_000L, 26_000_000L);
        PortfolioSummary summary = PortfolioSummaryCalculator.summarize(List.of(coded), List.of(august, september), TODAY, true);

        // when

        // then
        assertThat(summary.assets().get(0).profitRate()).isEqualTo((14_300_000d - 11_800_000d) * 100d / 11_800_000d);
        assertThat(summary.history()).extracting(PortfolioSummary.HistoryRow::yearMonth)
                .containsExactly("2026-09", "2026-08");
        assertThat(summary.history().get(0).delta()).isEqualTo(1_980_000L);
        assertThat(summary.history().get(0).status()).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("비중이 큰 자산은 경고 문구에 이름을 넣는다")
    void putsTheLargestHoldingNameInTheWarning() {
        // given
        PortfolioSummary summary = PortfolioSummaryCalculator.summarize(sampleHoldings(), List.of(), TODAY, false);

        // when

        // then
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
