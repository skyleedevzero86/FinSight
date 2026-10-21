package com.sleekydz86.finsight.core.economypick.service;

import com.sleekydz86.finsight.core.economypick.adapter.YahooChartClient;
import com.sleekydz86.finsight.core.economypick.domain.ChartWindow;
import com.sleekydz86.finsight.core.economypick.domain.EconomyPickChart;
import com.sleekydz86.finsight.core.economypick.domain.PriceBar;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EconomyPickChartService {

    private static final int TAIL = 80;

    private final YahooChartClient yahooChartClient;

    public EconomyPickChartService(YahooChartClient yahooChartClient) {
        this.yahooChartClient = yahooChartClient;
    }

    public EconomyPickChart load(String symbol, String market, String period) {
        ChartWindow window = ChartWindow.of(period);
        String yahoo = ChartWindow.yahooSymbol(symbol, market);
        List<PriceBar> bars = yahoo.isBlank() ? List.of() : yahooChartClient.chart(yahoo, window.range(), window.interval());
        return new EconomyPickChart(symbol == null ? "" : symbol.trim(), window.label(), tail(bars));
    }

    private List<PriceBar> tail(List<PriceBar> bars) {
        if (bars.size() <= TAIL) {
            return bars;
        }
        return List.copyOf(bars.subList(bars.size() - TAIL, bars.size()));
    }
}
