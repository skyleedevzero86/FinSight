package com.sleekydz86.finsight.core.economypick.domain;

import java.util.List;

public final class QuantMath {

    private QuantMath() {
    }

    public record Regime(int regime, String label, String signal) {
    }

    public record Hedge(int longWeight, int hedge, int safe) {
    }

    public static Regime regime(double price, double ma20, double ma60, double rsi, double volume, double volumeSma, double vix) {
        if (vix < 15 && price > ma20 && rsi > 70 && volume > volumeSma * 2) {
            return new Regime(3, "급등", "BUY");
        }
        if (vix < 20 && price > ma20 && rsi > 50) {
            return new Regime(2, "상승", "BUY");
        }
        if (vix > 20 && vix < 30 && ma60 != 0 && Math.abs(ma20 - ma60) / ma60 < 0.05 && rsi > 30 && rsi < 70) {
            return new Regime(1, "횡보", "HOLD");
        }
        if (vix > 30 && price < ma60 && rsi < 30) {
            return new Regime(0, "하락", "SELL");
        }
        if (rsi > 50) {
            return new Regime(2, "상승", "BUY");
        }
        return new Regime(1, "횡보", "HOLD");
    }

    public static double momentum(List<Double> prices, int period) {
        if (prices == null || prices.size() <= period || period < 1) {
            return 0;
        }
        double current = prices.get(prices.size() - 1);
        double past = prices.get(prices.size() - 1 - period);
        if (past == 0) {
            return 0;
        }
        return ((current - past) / past) * 100;
    }

    public static String momentumSignal(double momentum) {
        if (momentum > 5) {
            return "BUY";
        }
        if (momentum < -5) {
            return "SELL";
        }
        return "HOLD";
    }

    public static Hedge hedge(int regime) {
        if (regime == 0) {
            return new Hedge(65, 35, 0);
        }
        if (regime == 1) {
            return new Hedge(65, 15, 20);
        }
        if (regime == 2) {
            return new Hedge(80, 0, 20);
        }
        return new Hedge(90, 0, 10);
    }

    public static double rsi(List<Double> prices, int period) {
        if (prices == null || prices.size() < period + 1 || period < 1) {
            return 50;
        }
        double gain = 0;
        double loss = 0;
        for (int index = prices.size() - period; index < prices.size(); index++) {
            double diff = prices.get(index) - prices.get(index - 1);
            if (diff > 0) {
                gain += diff;
            } else {
                loss -= diff;
            }
        }
        if (loss == 0) {
            return 100;
        }
        double rs = (gain / period) / (loss / period);
        return 100 - (100 / (1 + rs));
    }

    public static double movingAverage(List<Double> prices, int period) {
        if (prices == null || prices.isEmpty()) {
            return 0;
        }
        if (prices.size() < period || period < 1) {
            return prices.get(prices.size() - 1);
        }
        double sum = 0;
        for (int index = prices.size() - period; index < prices.size(); index++) {
            sum += prices.get(index);
        }
        return sum / period;
    }

    public static int amqsScore(double momentum, double rsi, double price, double ma50) {
        int score = momentumScore(momentum) + rsiScore(rsi);
        score += price > ma50 ? 20 : -20;
        return Math.max(-100, Math.min(100, score));
    }

    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static int momentumScore(double momentum) {
        if (momentum > 15) {
            return 40;
        }
        if (momentum > 5) {
            return 20;
        }
        if (momentum < -15) {
            return -40;
        }
        if (momentum < -5) {
            return -20;
        }
        return 0;
    }

    private static int rsiScore(double rsi) {
        if (rsi > 60) {
            return 20;
        }
        if (rsi < 40) {
            return -20;
        }
        return 0;
    }
}
