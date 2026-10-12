package com.sleekydz86.finsight.core.economypick.domain;

import java.util.ArrayList;
import java.util.List;

public final class Bars {

    private Bars() {
    }

    public static List<Double> closes(List<PriceBar> bars) {
        List<Double> values = new ArrayList<>();
        if (bars == null) {
            return values;
        }
        for (PriceBar bar : bars) {
            values.add(bar.close());
        }
        return values;
    }

    public static List<Double> volumes(List<PriceBar> bars) {
        List<Double> values = new ArrayList<>();
        if (bars == null) {
            return values;
        }
        for (PriceBar bar : bars) {
            values.add(bar.volume());
        }
        return values;
    }
}
