package com.sleekydz86.finsight.core.economypick.domain;

import java.util.List;

public record QuantSnapshot(
        String regimeLabel,
        String regimeSignal,
        int regime,
        double vix,
        QuantMath.Hedge hedge,
        List<Stock> amqs,
        String amqsSignal,
        double fearGreed,
        String fearGreedStatus,
        HynixPredictMath.Prediction hynix) {

    public record Stock(String ticker, String name, String price, String changePct, double momentum, int rsi, String signal, int score) {
    }
}
