package com.sleekydz86.finsight.core.economypick.domain;

public final class AssetRatioMath {

    private AssetRatioMath() {
    }

    public static EconomyPickAssets owned(long assets, long liabilities) {
        if (assets <= 0 && liabilities <= 0) {
            return null;
        }
        double debt = round1(liabilities * 100.0 / (assets + liabilities));
        double equity = round1(100 - debt);
        return new EconomyPickAssets("MINE", equity, debt, "내 포트폴리오 자산·부채 비율입니다.");
    }

    public static EconomyPickAssets sample(int debtPercent) {
        int debt = Math.max(8, Math.min(55, debtPercent));
        return new EconomyPickAssets("SAMPLE", 100 - debt, debt, "보유 자산이 없어 예시 비율을 보여 줍니다.");
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
