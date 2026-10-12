package com.sleekydz86.finsight.core.economypick.domain;

import java.util.List;

public record EconomyPickBoard(
        List<PickRow> rows,
        List<Tape> tickers,
        KosdaqDashboard dashboard,
        QuantSnapshot quant,
        SpaceXSnapshot spacex,
        TrumpSnapshot trump) {

    public record Tape(String symbol, String name, String price, String changePct, String direction, String logo) {
    }
}
