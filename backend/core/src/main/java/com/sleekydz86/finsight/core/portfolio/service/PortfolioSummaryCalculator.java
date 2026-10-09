package com.sleekydz86.finsight.core.portfolio.service;

import com.sleekydz86.finsight.core.portfolio.domain.PortfolioAssetCategory;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioAssetKind;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioHolding;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioMonthPoint;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioSummary;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioSummary.AllocationSlice;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioSummary.AssetRow;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioSummary.HistoryRow;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioSummary.InsightLine;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioSummary.TrendBar;

import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class PortfolioSummaryCalculator {

    private static final PortfolioAssetCategory[] ALLOCATION_ORDER = {
            PortfolioAssetCategory.CASH,
            PortfolioAssetCategory.DEPOSIT,
            PortfolioAssetCategory.STOCK_ETF,
            PortfolioAssetCategory.PENSION,
            PortfolioAssetCategory.REAL_ESTATE,
            PortfolioAssetCategory.CAR,
            PortfolioAssetCategory.CRYPTO,
            PortfolioAssetCategory.OTHER
    };

    private PortfolioSummaryCalculator() {
    }

    public static PortfolioSummary summarize(
            List<PortfolioHolding> holdings,
            List<PortfolioMonthPoint> history,
            YearMonth today,
            boolean recordedThisMonth) {
        List<PortfolioHolding> rows = holdings == null ? List.of() : List.copyOf(holdings);
        List<PortfolioMonthPoint> points = ordered(history);
        Map<YearMonth, PortfolioMonthPoint> byMonth = index(points);
        long totalAssets = sum(rows, PortfolioAssetKind.ASSET);
        long totalLiabilities = sum(rows, PortfolioAssetKind.LIABILITY);
        long netWorth = totalAssets - totalLiabilities;
        Long pace = averageMonthlyGain(points);
        return new PortfolioSummary(
                totalAssets,
                totalLiabilities,
                netWorth,
                PortfolioSummary.GOAL_AMOUNT,
                percentTenths(Math.max(netWorth, 0), PortfolioSummary.GOAL_AMOUNT),
                monthDelta(byMonth, today, netWorth),
                yearDelta(points, today, netWorth),
                pace,
                expectedGoalLabel(pace, today, netWorth),
                totalAssets == 0 ? null : percentTenths(totalLiabilities, totalAssets),
                allocation(rows, totalAssets),
                trend(byMonth, today, netWorth),
                assetRows(rows, totalAssets),
                insights(rows, byMonth, today, totalAssets, totalLiabilities, netWorth),
                advice(rows, totalAssets),
                recordedThisMonth,
                historyRows(points));
    }

    static int percentTenths(long part, long whole) {
        if (part <= 0 || whole <= 0) {
            return 0;
        }
        return (int) Math.round(part * 1000.0 / whole);
    }

    private static long sum(List<PortfolioHolding> holdings, PortfolioAssetKind kind) {
        long total = 0;
        for (PortfolioHolding holding : holdings) {
            if (holding.kind() == kind) {
                total = add(total, holding.amount());
            }
        }
        return total;
    }

    private static long add(long left, long right) {
        if (right <= 0) {
            return left;
        }
        if (left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private static Long monthDelta(Map<YearMonth, PortfolioMonthPoint> byMonth, YearMonth today, long netWorth) {
        PortfolioMonthPoint previous = byMonth.get(today.minusMonths(1));
        if (previous == null) {
            return null;
        }
        return netWorth - previous.netWorth();
    }

    private static Long yearDelta(List<PortfolioMonthPoint> history, YearMonth today, long netWorth) {
        YearMonth january = YearMonth.of(today.getYear(), 1);
        PortfolioMonthPoint baseline = latestBefore(history, january);
        if (baseline == null) {
            baseline = earliestFrom(history, january, today);
        }
        if (baseline == null) {
            return null;
        }
        return netWorth - baseline.netWorth();
    }

    private static Long averageMonthlyGain(List<PortfolioMonthPoint> history) {
        if (history.size() < 2) {
            return null;
        }
        PortfolioMonthPoint first = history.get(0);
        PortfolioMonthPoint last = history.get(history.size() - 1);
        long months = ChronoUnit.MONTHS.between(first.month(), last.month());
        if (months <= 0) {
            return null;
        }
        return (last.netWorth() - first.netWorth()) / months;
    }

    private static String expectedGoalLabel(Long pace, YearMonth today, long netWorth) {
        if (pace == null || pace <= 0 || netWorth >= PortfolioSummary.GOAL_AMOUNT) {
            return null;
        }
        long remain = PortfolioSummary.GOAL_AMOUNT - netWorth;
        long months = (remain + pace - 1) / pace;
        return today.plusMonths(months).toString().replace('-', '.');
    }

    private static List<AllocationSlice> allocation(List<PortfolioHolding> holdings, long totalAssets) {
        if (totalAssets <= 0) {
            return List.of();
        }
        EnumMap<PortfolioAssetCategory, Long> amounts = amountsByCategory(holdings);
        List<AllocationSlice> slices = roundedSlices(amounts, totalAssets);
        return List.copyOf(slices);
    }

    private static EnumMap<PortfolioAssetCategory, Long> amountsByCategory(List<PortfolioHolding> holdings) {
        EnumMap<PortfolioAssetCategory, Long> amounts = new EnumMap<>(PortfolioAssetCategory.class);
        for (PortfolioHolding holding : holdings) {
            if (holding.kind() != PortfolioAssetKind.ASSET) {
                continue;
            }
            amounts.merge(holding.category(), holding.amount(), PortfolioSummaryCalculator::add);
        }
        return amounts;
    }

    private static List<AllocationSlice> roundedSlices(EnumMap<PortfolioAssetCategory, Long> amounts, long totalAssets) {
        List<AllocationSlice> slices = new ArrayList<>();
        int largest = 0;
        int used = 0;
        for (PortfolioAssetCategory category : ALLOCATION_ORDER) {
            long amount = amounts.getOrDefault(category, 0L);
            if (amount <= 0) {
                continue;
            }
            int tenths = percentTenths(amount, totalAssets);
            slices.add(new AllocationSlice(category.name(), category.label(), amount, tenths));
            if (amount > amounts.getOrDefault(categoryOf(slices.get(largest)), 0L)) {
                largest = slices.size() - 1;
            }
            used += tenths;
        }
        return adjustRemainder(slices, used, largest);
    }

    private static PortfolioAssetCategory categoryOf(AllocationSlice slice) {
        return PortfolioAssetCategory.valueOf(slice.category());
    }

    private static List<AllocationSlice> adjustRemainder(List<AllocationSlice> slices, int used, int largest) {
        if (slices.isEmpty() || used == 1000) {
            return slices;
        }
        AllocationSlice slice = slices.get(largest);
        int tenths = Math.max(0, slice.percentTenths() + (1000 - used));
        slices.set(largest, new AllocationSlice(slice.category(), slice.label(), slice.amount(), tenths));
        return slices;
    }

    private static List<TrendBar> trend(Map<YearMonth, PortfolioMonthPoint> byMonth, YearMonth today, long liveNet) {
        List<YearMonth> months = window(today);
        List<Long> values = months.stream().map(month -> barValue(byMonth, today, liveNet, month)).toList();
        long max = values.stream().mapToLong(Long::longValue).max().orElse(0L);
        List<TrendBar> bars = new ArrayList<>();
        for (int index = 0; index < months.size(); index++) {
            long value = values.get(index);
            bars.add(new TrendBar(months.get(index).getMonthValue() + "월", value, height(value, max)));
        }
        return List.copyOf(bars);
    }

    private static List<YearMonth> window(YearMonth today) {
        List<YearMonth> months = new ArrayList<>();
        for (int offset = 9; offset >= 0; offset--) {
            months.add(today.minusMonths(offset));
        }
        return months;
    }

    private static long barValue(Map<YearMonth, PortfolioMonthPoint> byMonth, YearMonth today, long liveNet, YearMonth month) {
        if (month.equals(today)) {
            return Math.max(liveNet, 0);
        }
        PortfolioMonthPoint point = byMonth.get(month);
        return point == null ? 0L : Math.max(point.netWorth(), 0);
    }

    private static int height(long value, long max) {
        if (value <= 0 || max <= 0) {
            return 0;
        }
        return (int) Math.max(8, Math.round(value * 100.0 / max));
    }

    private static List<AssetRow> assetRows(List<PortfolioHolding> holdings, long totalAssets) {
        Comparator<PortfolioHolding> byAmount = Comparator.comparingLong(PortfolioHolding::amount).reversed()
                .thenComparing(PortfolioHolding::id, Comparator.nullsLast(Long::compareTo));
        List<AssetRow> rows = new ArrayList<>();
        holdings.stream().filter(holding -> holding.kind() == PortfolioAssetKind.ASSET).sorted(byAmount).map(holding -> toRow(holding, totalAssets)).forEach(rows::add);
        holdings.stream().filter(holding -> holding.kind() == PortfolioAssetKind.LIABILITY).sorted(byAmount).map(holding -> toRow(holding, totalAssets)).forEach(rows::add);
        return List.copyOf(rows);
    }

    private static Double resolvedProfit(PortfolioHolding holding) {
        if (holding.profitRate() != null) {
            return holding.profitRate();
        }
        if (holding.acquisitionAmount() == null || holding.acquisitionAmount() <= 0) {
            return null;
        }
        return (holding.amount() - holding.acquisitionAmount()) * 100.0 / holding.acquisitionAmount();
    }

    private static List<HistoryRow> historyRows(List<PortfolioMonthPoint> points) {
        List<HistoryRow> rows = new ArrayList<>();
        Long previous = null;
        for (PortfolioMonthPoint point : points) {
            Long delta = previous == null ? null : point.netWorth() - previous;
            rows.add(new HistoryRow(point.month().toString(), point.netWorth(), delta, "CONFIRMED"));
            previous = point.netWorth();
        }
        java.util.Collections.reverse(rows);
        return List.copyOf(rows);
    }

    private static AssetRow toRow(PortfolioHolding holding, long totalAssets) {
        return new AssetRow(
                holding.id(),
                holding.name(),
                holding.kind().name(),
                holding.category().name(),
                holding.category().label(),
                holding.amount(),
                resolvedProfit(holding),
                percentTenths(holding.amount(), totalAssets));
    }

    private static List<InsightLine> insights(
            List<PortfolioHolding> holdings,
            Map<YearMonth, PortfolioMonthPoint> byMonth,
            YearMonth today,
            long totalAssets,
            long totalLiabilities,
            long netWorth) {
        if (holdings.isEmpty() || totalAssets <= 0) {
            return List.of();
        }
        List<InsightLine> lines = new ArrayList<>();
        addGrowth(lines, byMonth, today, netWorth);
        addDebt(lines, byMonth, today, totalLiabilities);
        addCash(lines, holdings, totalAssets);
        addConcentration(lines, holdings, totalAssets);
        return List.copyOf(lines);
    }

    private static void addGrowth(List<InsightLine> lines, Map<YearMonth, PortfolioMonthPoint> byMonth, YearMonth today, long netWorth) {
        if (steadyGrowth(byMonth, today, netWorth)) {
            lines.add(new InsightLine("GOOD", "최근 6개월 순자산이 꾸준히 증가했습니다."));
        }
    }

    private static void addDebt(List<InsightLine> lines, Map<YearMonth, PortfolioMonthPoint> byMonth, YearMonth today, long liabilities) {
        if (debtFalling(byMonth, today, liabilities)) {
            lines.add(new InsightLine("GOOD", "부채가 최근 3개월 연속 감소 중입니다."));
        }
    }

    private static void addCash(List<InsightLine> lines, List<PortfolioHolding> holdings, long totalAssets) {
        int cash = percentTenths(sumCategory(holdings, PortfolioAssetCategory.CASH), totalAssets);
        if (cash < 150) {
            lines.add(new InsightLine("WARN", "현금성 자산은 총자산의 " + formatTenths(cash) + "입니다."));
        }
    }

    private static void addConcentration(List<InsightLine> lines, List<PortfolioHolding> holdings, long totalAssets) {
        PortfolioAssetCategory top = null;
        long topAmount = 0;
        for (PortfolioAssetCategory category : ALLOCATION_ORDER) {
            long amount = sumCategory(holdings, category);
            if (amount > topAmount) {
                top = category;
                topAmount = amount;
            }
        }
        if (top == null) {
            return;
        }
        int share = percentTenths(topAmount, totalAssets);
        if (share >= 250) {
            lines.add(new InsightLine("WARN", top.label() + " 비중이 " + formatTenths(share) + "로 높습니다."));
        }
    }

    private static String advice(List<PortfolioHolding> holdings, long totalAssets) {
        if (holdings.isEmpty() || totalAssets <= 0) {
            return "자산을 등록하면 순자산과 구성이 계산됩니다.";
        }
        int cash = percentTenths(sumCategory(holdings, PortfolioAssetCategory.CASH), totalAssets);
        if (cash < 150) {
            return "현금성 자산을 먼저 늘린 뒤 추가 투자를 고려하는 편이 재무 안정성에 유리합니다.";
        }
        return "현재 보유 금액으로 1억 목표 진행률을 계산했습니다.";
    }

    private static boolean steadyGrowth(Map<YearMonth, PortfolioMonthPoint> byMonth, YearMonth today, long netWorth) {
        long previous = Long.MIN_VALUE;
        for (int offset = 5; offset >= 0; offset--) {
            Long value = monthNet(byMonth, today, netWorth, today.minusMonths(offset));
            if (value == null || value < previous) {
                return false;
            }
            previous = value;
        }
        return true;
    }

    private static Long monthNet(Map<YearMonth, PortfolioMonthPoint> byMonth, YearMonth today, long liveNet, YearMonth month) {
        if (month.equals(today)) {
            return liveNet;
        }
        PortfolioMonthPoint point = byMonth.get(month);
        return point == null ? null : point.netWorth();
    }

    private static boolean debtFalling(Map<YearMonth, PortfolioMonthPoint> byMonth, YearMonth today, long liabilities) {
        PortfolioMonthPoint older = byMonth.get(today.minusMonths(2));
        PortfolioMonthPoint middle = byMonth.get(today.minusMonths(1));
        if (older == null || middle == null) {
            return false;
        }
        return older.liabilities() > middle.liabilities() && middle.liabilities() > liabilities;
    }

    private static long sumCategory(List<PortfolioHolding> holdings, PortfolioAssetCategory category) {
        long total = 0;
        for (PortfolioHolding holding : holdings) {
            if (holding.kind() == PortfolioAssetKind.ASSET && holding.category() == category) {
                total = add(total, holding.amount());
            }
        }
        return total;
    }

    private static String formatTenths(int tenths) {
        int whole = tenths / 10;
        int fraction = Math.abs(tenths % 10);
        if (fraction == 0) {
            return whole + "%";
        }
        return whole + "." + fraction + "%";
    }

    private static List<PortfolioMonthPoint> ordered(List<PortfolioMonthPoint> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        return history.stream().sorted(Comparator.comparing(PortfolioMonthPoint::month)).toList();
    }

    private static Map<YearMonth, PortfolioMonthPoint> index(List<PortfolioMonthPoint> history) {
        Map<YearMonth, PortfolioMonthPoint> byMonth = new java.util.LinkedHashMap<>();
        for (PortfolioMonthPoint point : history) {
            byMonth.put(point.month(), point);
        }
        return byMonth;
    }

    private static PortfolioMonthPoint latestBefore(List<PortfolioMonthPoint> history, YearMonth bound) {
        PortfolioMonthPoint found = null;
        for (PortfolioMonthPoint point : history) {
            if (point.month().isBefore(bound)) {
                found = point;
            }
        }
        return found;
    }

    private static PortfolioMonthPoint earliestFrom(List<PortfolioMonthPoint> history, YearMonth from, YearMonth until) {
        for (PortfolioMonthPoint point : history) {
            if (!point.month().isBefore(from) && point.month().isBefore(until)) {
                return point;
            }
        }
        return null;
    }
}
