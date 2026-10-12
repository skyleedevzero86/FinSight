package com.sleekydz86.finsight.core.economypick.service;

import com.sleekydz86.finsight.core.economypick.domain.PickRow;
import com.sleekydz86.finsight.core.economypick.domain.PriceBar;
import com.sleekydz86.finsight.core.economypick.domain.QuoteText;
import com.sleekydz86.finsight.core.economypick.domain.SpaceXMath;
import com.sleekydz86.finsight.core.economypick.domain.SpaceXSnapshot;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SpaceXQuantService {

    private static final Map<String, String[]> UNIVERSE = universe();

    private final QuoteBook quoteBook;

    public SpaceXQuantService(QuoteBook quoteBook) {
        this.quoteBook = quoteBook;
    }

    public List<String> symbols() {
        return List.copyOf(UNIVERSE.keySet());
    }

    public SpaceXSnapshot current() {
        return snapshot(quoteBook.load(symbols()));
    }

    public SpaceXSnapshot snapshot(Map<String, List<PriceBar>> book) {
        List<SpaceXSnapshot.Stock> stocks = new ArrayList<>();
        for (Map.Entry<String, String[]> entry : UNIVERSE.entrySet()) {
            stocks.add(stock(entry.getKey(), entry.getValue()[0], entry.getValue()[1], quoteBook.bars(book, entry.getKey())));
        }
        return new SpaceXSnapshot(stocks);
    }

    public List<PickRow> rows(SpaceXSnapshot snapshot) {
        List<PickRow> rows = new ArrayList<>();
        for (SpaceXSnapshot.Stock stock : snapshot.stocks()) {
            rows.add(row(stock));
        }
        return rows;
    }

    private SpaceXSnapshot.Stock stock(String symbol, String name, String group, List<PriceBar> bars) {
        SpaceXMath.ScoreCard card = SpaceXMath.score(bars);
        if (bars.size() < 2) {
            return new SpaceXSnapshot.Stock(symbol, name, groupLabel(group), "—", "—", card.signal(), card.score(),
                    card.rsi(), card.williamsR(), "—", "—", "—");
        }
        double previous = bars.get(bars.size() - 2).close();
        double change = previous == 0 ? 0 : ((bars.get(bars.size() - 1).close() - previous) / previous) * 100;
        return new SpaceXSnapshot.Stock(
                symbol, name, groupLabel(group), QuoteText.usd(bars.get(bars.size() - 1).close()),
                QuoteText.percent(change), card.signal(), card.score(), card.rsi(), card.williamsR(),
                extreme(bars, true), extreme(bars, false), QuoteText.number(bars.get(bars.size() - 1).volume()));
    }

    private PickRow row(SpaceXSnapshot.Stock stock) {
        return new PickRow(
                "SPACEX",
                stock.name(),
                stock.symbol(),
                stock.price(),
                stock.changePct(),
                direction(stock.changePct()),
                QuoteText.logo(stock.symbol()),
                "SpaceX",
                stock.volume(),
                stock.group() + " · " + stock.signal() + " " + stock.score(),
                List.of(stock.price(), stock.signal(), blank(stock.volume()), rsi(stock.rsi()),
                        "SpaceX", stock.group(), blank(stock.low52w()), blank(stock.high52w())));
    }

    private String extreme(List<PriceBar> bars, boolean high) {
        double value = high ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        for (PriceBar bar : bars) {
            value = high ? Math.max(value, bar.high()) : Math.min(value, bar.low());
        }
        return QuoteText.usd(value);
    }

    private String direction(String change) {
        if (change != null && change.startsWith("+")) {
            return "up";
        }
        if (change != null && change.startsWith("-")) {
            return "down";
        }
        return "zero";
    }

    private String rsi(Double rsi) {
        return rsi == null ? "—" : "RSI " + Math.round(rsi);
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private String groupLabel(String group) {
        return switch (group) {
            case "pure_space" -> "순수 우주";
            case "etf" -> "ETF";
            case "defense" -> "방산";
            default -> group;
        };
    }

    private static Map<String, String[]> universe() {
        Map<String, String[]> rows = new LinkedHashMap<>();
        rows.put("RKLB", new String[]{"로켓랩", "pure_space"});
        rows.put("ASTS", new String[]{"AST 스페이스모바일", "pure_space"});
        rows.put("LUNR", new String[]{"인튜이티브 머신", "pure_space"});
        rows.put("SPCE", new String[]{"버진 갤럭틱", "pure_space"});
        rows.put("RDW", new String[]{"레드와이어", "pure_space"});
        rows.put("ASTR", new String[]{"아스트라", "pure_space"});
        rows.put("SPCX", new String[]{"SpaceX", "pure_space"});
        rows.put("ARKX", new String[]{"ARK 우주 ETF", "etf"});
        rows.put("LMT", new String[]{"록히드마틴", "defense"});
        rows.put("NOC", new String[]{"노스롭그루먼", "defense"});
        rows.put("RTX", new String[]{"레이시온", "defense"});
        rows.put("BA", new String[]{"보잉", "defense"});
        return Map.copyOf(rows);
    }
}
