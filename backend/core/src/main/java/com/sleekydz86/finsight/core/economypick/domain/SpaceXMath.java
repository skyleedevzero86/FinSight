package com.sleekydz86.finsight.core.economypick.domain;

import java.util.List;

public final class SpaceXMath {

    private SpaceXMath() {
    }

    public record ScoreCard(Double williamsR, Double rsi, Double sma20, Double sma50, Double momentum, double score, String signal) {
    }

    public static Double williamsR(List<PriceBar> bars, int period) {
        if (bars == null || bars.size() < period) {
            return null;
        }
        List<PriceBar> slice = bars.subList(bars.size() - period, bars.size());
        double highest = slice.stream().mapToDouble(PriceBar::high).max().orElse(0);
        double lowest = slice.stream().mapToDouble(PriceBar::low).min().orElse(0);
        double close = bars.get(bars.size() - 1).close();
        if (highest == lowest) {
            return -50d;
        }
        return ((highest - close) / (highest - lowest)) * -100;
    }

    public static Double rsi(List<PriceBar> bars, int period) {
        if (bars == null || bars.size() < period + 1) {
            return null;
        }
        double gains = 0;
        double losses = 0;
        for (int index = bars.size() - period; index < bars.size(); index++) {
            double diff = bars.get(index).close() - bars.get(index - 1).close();
            if (diff > 0) {
                gains += diff;
            } else {
                losses -= diff;
            }
        }
        if (losses == 0) {
            return 100d;
        }
        double rs = gains / losses;
        return 100 - 100 / (1 + rs);
    }

    public static Double sma(List<PriceBar> bars, int period) {
        if (bars == null || bars.size() < period) {
            return null;
        }
        double sum = 0;
        for (int index = bars.size() - period; index < bars.size(); index++) {
            sum += bars.get(index).close();
        }
        return sum / period;
    }

    public static Double momentum(List<PriceBar> bars, int period) {
        if (bars == null || bars.size() < period + 1) {
            return null;
        }
        double current = bars.get(bars.size() - 1).close();
        double past = bars.get(bars.size() - 1 - period).close();
        if (past == 0) {
            return null;
        }
        return ((current - past) / past) * 100;
    }

    public static ScoreCard score(List<PriceBar> bars) {
        if (bars == null || bars.isEmpty()) {
            return new ScoreCard(null, null, null, null, null, 0, "HOLD");
        }
        Double williams = williamsR(bars, 14);
        Double rsi = rsi(bars, 14);
        Double sma20 = sma(bars, 20);
        Double sma50 = sma(bars, 50);
        Double momentum = momentum(bars, 20);
        double close = bars.get(bars.size() - 1).close();
        double score = scoreValue(williams, rsi, close, sma20, sma50, momentum);
        return new ScoreCard(round(williams), round(rsi), round(sma20), round(sma50), round(momentum), score, signal(score));
    }

    public static double scoreValue(Double williams, Double rsi, double close, Double sma20, Double sma50, Double momentum) {
        double score = 0;
        score += williamsPart(williams);
        score += rsiPart(rsi);
        if (sma20 != null) {
            score += close > sma20 ? 0.5 : -0.5;
        }
        if (sma50 != null) {
            score += close > sma50 ? 0.5 : -0.5;
        }
        score += momentumPart(momentum);
        return Math.max(-3, Math.min(3, Math.round(score * 10) / 10.0));
    }

    public static String signal(double score) {
        if (score >= 1.5) {
            return "BUY";
        }
        if (score <= -1.5) {
            return "SELL";
        }
        if (score <= -0.5) {
            return "AVOID";
        }
        return "HOLD";
    }

    private static double williamsPart(Double williams) {
        if (williams == null) {
            return 0;
        }
        if (williams <= -80) {
            return 1;
        }
        if (williams >= -20) {
            return -1;
        }
        if (williams <= -60) {
            return 0.5;
        }
        return 0;
    }

    private static double rsiPart(Double rsi) {
        if (rsi == null) {
            return 0;
        }
        if (rsi < 30) {
            return 1;
        }
        if (rsi > 70) {
            return -1;
        }
        if (rsi < 45) {
            return 0.3;
        }
        if (rsi > 55) {
            return -0.3;
        }
        return 0;
    }

    private static double momentumPart(Double momentum) {
        if (momentum == null) {
            return 0;
        }
        if (momentum > 15) {
            return 0.5;
        }
        if (momentum > 5) {
            return 0.2;
        }
        if (momentum < -15) {
            return -0.5;
        }
        if (momentum < -5) {
            return -0.2;
        }
        return 0;
    }

    private static Double round(Double value) {
        if (value == null) {
            return null;
        }
        return QuantMath.round2(value);
    }
}
