package com.sleekydz86.finsight.core.economypick.domain;

import java.util.ArrayList;
import java.util.List;

public final class HynixPredictMath {

    private HynixPredictMath() {
    }

    public record Prediction(
            double muPrice,
            double muChangePct,
            String muSignal,
            double hynixPrevClose,
            double hynixPredictedOpen,
            double hynixPredictedChangePct,
            String hynixSignal,
            double beta,
            double r2,
            int points) {
    }

    public static Prediction predict(List<Double> muClose, List<Double> hynixClose) {
        if (muClose == null || hynixClose == null || muClose.size() < 6 || hynixClose.size() < 2) {
            return null;
        }
        double muCurrent = muClose.get(muClose.size() - 1);
        double muPrev = muClose.get(muClose.size() - 2);
        double muChange = muPrev == 0 ? 0 : ((muCurrent - muPrev) / muPrev) * 100;
        double hynixPrev = hynixClose.get(hynixClose.size() - 1);
        List<Double> muReturns = tailReturns(muClose, 21);
        List<Double> hynixReturns = tailReturns(hynixClose, 21);
        double beta = beta(muReturns, hynixReturns);
        double r2 = r2(muReturns, hynixReturns, beta);
        double predictedChange = beta * muChange;
        return new Prediction(
                QuantMath.round2(muCurrent),
                QuantMath.round2(muChange),
                signal(muChange),
                QuantMath.round2(hynixPrev),
                QuantMath.round2(hynixPrev * (1 + predictedChange / 100)),
                QuantMath.round2(predictedChange),
                signal(predictedChange),
                QuantMath.round2(beta),
                QuantMath.round2(r2),
                Math.min(muReturns.size(), hynixReturns.size()));
    }

    public static double beta(List<Double> xReturns, List<Double> yReturns) {
        int size = Math.min(sizeOf(xReturns), sizeOf(yReturns));
        if (size < 5) {
            return 0.15;
        }
        double xMean = mean(xReturns, size);
        double yMean = mean(yReturns, size);
        double numerator = 0;
        double denominator = 0;
        for (int index = 0; index < size; index++) {
            double x = xReturns.get(xReturns.size() - size + index) - xMean;
            double y = yReturns.get(yReturns.size() - size + index) - yMean;
            numerator += x * y;
            denominator += x * x;
        }
        return denominator == 0 ? 1.2 : Math.abs(numerator / denominator);
    }

    private static double r2(List<Double> xReturns, List<Double> yReturns, double beta) {
        int size = Math.min(sizeOf(xReturns), sizeOf(yReturns));
        if (size < 2) {
            return 0;
        }
        double yMean = mean(yReturns, size);
        double residual = 0;
        double total = 0;
        for (int index = 0; index < size; index++) {
            double x = xReturns.get(xReturns.size() - size + index);
            double y = yReturns.get(yReturns.size() - size + index);
            double gap = y - (beta * x);
            residual += gap * gap;
            double drift = y - yMean;
            total += drift * drift;
        }
        return total == 0 ? 0 : Math.max(0, 1 - residual / total);
    }

    private static List<Double> tailReturns(List<Double> prices, int window) {
        int start = Math.max(1, prices.size() - window);
        List<Double> rows = new ArrayList<>();
        for (int index = start; index < prices.size(); index++) {
            double previous = prices.get(index - 1);
            rows.add(previous == 0 ? 0 : (prices.get(index) - previous) / previous);
        }
        return rows;
    }

    private static double mean(List<Double> values, int size) {
        double sum = 0;
        for (int index = values.size() - size; index < values.size(); index++) {
            sum += values.get(index);
        }
        return sum / size;
    }

    private static int sizeOf(List<Double> values) {
        return values == null ? 0 : values.size();
    }

    private static String signal(double change) {
        if (change > 0.15) {
            return "상승";
        }
        if (change < -0.15) {
            return "하락";
        }
        return "보합";
    }
}
