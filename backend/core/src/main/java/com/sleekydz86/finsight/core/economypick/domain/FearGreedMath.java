package com.sleekydz86.finsight.core.economypick.domain;

import java.util.ArrayList;
import java.util.List;

public final class FearGreedMath {

    private static final double[] WEIGHTS = {0.25, 0.20, 0.20, 0.20, 0.15};

    private FearGreedMath() {
    }

    public record Score(double rsi, double ma, double vol, double mom, double volSurge, double total) {
    }

    public static Score score(List<Double> close, List<Double> volume, double spyReturn20d) {
        if (close == null || close.size() < 30 || volume == null || volume.size() < close.size()) {
            return null;
        }
        double rsiScore = rsiScore(close);
        double maScore = maScore(close);
        double volScore = volScore(close);
        double momScore = momScore(close, spyReturn20d);
        double surgeScore = surgeScore(volume);
        double total = rsiScore * WEIGHTS[0] + maScore * WEIGHTS[1] + volScore * WEIGHTS[2]
                + momScore * WEIGHTS[3] + surgeScore * WEIGHTS[4];
        return new Score(round(rsiScore), round(maScore), round(volScore), round(momScore), round(surgeScore), round(total));
    }

    public static String status(double score) {
        if (score < 40) {
            return "극단적 공포";
        }
        if (score < 50) {
            return "공포";
        }
        if (score < 60) {
            return "중립";
        }
        if (score < 80) {
            return "탐욕";
        }
        return "극단적 탐욕";
    }

    public static double return20(List<Double> close) {
        if (close == null || close.size() < 21 || close.get(close.size() - 21) == 0) {
            return 0;
        }
        return ((close.get(close.size() - 1) / close.get(close.size() - 21)) - 1) * 100;
    }

    private static double rsiScore(List<Double> close) {
        List<Double> gain = new ArrayList<>();
        List<Double> loss = new ArrayList<>();
        for (int index = 1; index < close.size(); index++) {
            double diff = close.get(index) - close.get(index - 1);
            gain.add(Math.max(diff, 0));
            loss.add(diff < 0 ? -diff : 0);
        }
        double avgGain = average(gain.subList(gain.size() - 14, gain.size()));
        double avgLoss = average(loss.subList(loss.size() - 14, loss.size()));
        double rs = avgLoss == 0 ? 100 : avgGain / avgLoss;
        double rsi = 100 - 100 / (1 + rs);
        return clip(((rsi - 30) / 40) * 100);
    }

    private static double maScore(List<Double> close) {
        double ma20 = average(close.subList(close.size() - 20, close.size()));
        double price = close.get(close.size() - 1);
        double pct = ma20 == 0 ? 0 : ((price / ma20) - 1) * 100;
        return clip(((pct + 5) / 10) * 100);
    }

    private static double volScore(List<Double> close) {
        List<Double> returns = returns(close);
        double hv20 = std(returns.subList(returns.size() - 20, returns.size())) * Math.sqrt(252);
        double hv60 = returns.size() >= 60
                ? std(returns.subList(returns.size() - 60, returns.size())) * Math.sqrt(252)
                : hv20;
        double ratio = hv60 == 0 ? 1 : hv20 / hv60;
        return clip(((1.2 - ratio) / 0.4) * 100);
    }

    private static double momScore(List<Double> close, double spyReturn20d) {
        return clip(((return20(close) - spyReturn20d + 10) / 20) * 100);
    }

    private static double surgeScore(List<Double> volume) {
        double avg = average(volume.subList(volume.size() - 20, volume.size()));
        double surge = avg == 0 ? 1 : volume.get(volume.size() - 1) / avg;
        return clip(((surge - 0.7) / 0.8) * 100);
    }

    private static List<Double> returns(List<Double> close) {
        List<Double> rows = new ArrayList<>();
        for (int index = 1; index < close.size(); index++) {
            double previous = close.get(index - 1);
            rows.add(previous == 0 ? 0 : (close.get(index) - previous) / previous);
        }
        return rows;
    }

    private static double average(List<Double> values) {
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return values.isEmpty() ? 0 : sum / values.size();
    }

    private static double std(List<Double> values) {
        if (values.size() < 2) {
            return 0;
        }
        double mean = average(values);
        double variance = 0;
        for (double value : values) {
            variance += (value - mean) * (value - mean);
        }
        return Math.sqrt(variance / (values.size() - 1));
    }

    private static double clip(double value) {
        return Math.max(0, Math.min(100, value));
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
