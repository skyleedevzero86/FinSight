package com.sleekydz86.finsight.core.economypick.service;

import com.sleekydz86.finsight.core.economypick.domain.Bars;
import com.sleekydz86.finsight.core.economypick.domain.EconomyPickBoard;
import com.sleekydz86.finsight.core.economypick.domain.FearGreedMath;
import com.sleekydz86.finsight.core.economypick.domain.HynixPredictMath;
import com.sleekydz86.finsight.core.economypick.domain.PickRow;
import com.sleekydz86.finsight.core.economypick.domain.PriceBar;
import com.sleekydz86.finsight.core.economypick.domain.QuantMath;
import com.sleekydz86.finsight.core.economypick.domain.QuantSnapshot;
import com.sleekydz86.finsight.core.economypick.domain.QuoteText;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class QuantBoardService {

    private static final Map<String, String> AMQS = ordered(
            "NVDA", "엔비디아", "AVGO", "브로드컴", "AMD", "AMD", "QCOM", "퀄컴",
            "ASML", "ASML", "MU", "마이크론", "TSM", "TSMC");
    private static final Map<String, String> INTEREST = ordered(
            "SPY", "S&P500", "QQQ", "나스닥100", "BTC-USD", "비트코인",
            "AAPL", "애플", "MSFT", "마이크로소프트", "NVDA", "엔비디아",
            "GOOGL", "알파벳", "META", "메타", "TSLA", "테슬라");
    private static final Map<String, String> SECTORS = ordered(
            "XLK", "기술", "XLF", "금융", "XLV", "헬스케어", "XLY", "경기소비",
            "XLP", "필수소비", "XLI", "산업", "XLB", "소재", "XLE", "에너지",
            "XLU", "유틸리티", "XLRE", "부동산");

    private final QuoteBook quoteBook;

    public QuantBoardService(QuoteBook quoteBook) {
        this.quoteBook = quoteBook;
    }

    public List<String> symbols() {
        List<String> symbols = new ArrayList<>();
        symbols.add("QQQ");
        symbols.add("^VIX");
        symbols.add("SPY");
        symbols.add("000660.KS");
        symbols.addAll(INTEREST.keySet());
        symbols.addAll(AMQS.keySet());
        symbols.addAll(SECTORS.keySet());
        return symbols;
    }

    public QuantSnapshot current() {
        return snapshot(quoteBook.load(symbols()));
    }

    public QuantSnapshot snapshot(Map<String, List<PriceBar>> book) {
        QuantMath.Regime regime = regime(book);
        List<QuantSnapshot.Stock> stocks = amqs(book);
        double fear = fearGreed(book);
        HynixPredictMath.Prediction hynix = HynixPredictMath.predict(
                Bars.closes(quoteBook.bars(book, "MU")),
                Bars.closes(quoteBook.bars(book, "000660.KS")));
        return new QuantSnapshot(
                regime.label(),
                regime.signal(),
                regime.regime(),
                vix(book),
                QuantMath.hedge(regime.regime()),
                stocks,
                overall(stocks),
                QuantMath.round2(fear),
                FearGreedMath.status(fear),
                hynix);
    }

    public List<PickRow> rows(QuantSnapshot snapshot, Map<String, List<PriceBar>> book) {
        List<PickRow> rows = new ArrayList<>();
        rows.add(regimeRow(snapshot, book));
        for (QuantSnapshot.Stock stock : snapshot.amqs()) {
            rows.add(stockRow(stock));
        }
        rows.add(fearRow(snapshot));
        if (snapshot.hynix() != null) {
            rows.add(hynixRow(snapshot.hynix()));
        }
        return rows;
    }

    public List<EconomyPickBoard.Tape> tapes(Map<String, List<PriceBar>> book) {
        List<EconomyPickBoard.Tape> tapes = new ArrayList<>();
        for (Map.Entry<String, String> entry : INTEREST.entrySet()) {
            addTape(tapes, book, entry.getKey(), entry.getValue());
        }
        return tapes;
    }

    private QuantMath.Regime regime(Map<String, List<PriceBar>> book) {
        List<Double> closes = Bars.closes(quoteBook.bars(book, "QQQ"));
        List<Double> volumes = Bars.volumes(quoteBook.bars(book, "QQQ"));
        if (closes.size() < 20) {
            return new QuantMath.Regime(1, "횡보", "HOLD");
        }
        double last = closes.get(closes.size() - 1);
        double volume = volumes.isEmpty() ? 0 : volumes.get(volumes.size() - 1);
        return QuantMath.regime(last, QuantMath.movingAverage(closes, 20), QuantMath.movingAverage(closes, 60),
                QuantMath.rsi(closes, 14), volume, average(volumes), vix(book));
    }

    private List<QuantSnapshot.Stock> amqs(Map<String, List<PriceBar>> book) {
        List<QuantSnapshot.Stock> rows = new ArrayList<>();
        for (Map.Entry<String, String> entry : AMQS.entrySet()) {
            rows.add(stock(entry.getKey(), entry.getValue(), quoteBook.bars(book, entry.getKey())));
        }
        return rows;
    }

    private QuantSnapshot.Stock stock(String ticker, String name, List<PriceBar> bars) {
        List<Double> closes = Bars.closes(bars);
        if (closes.size() < 21) {
            return new QuantSnapshot.Stock(ticker, name, "—", "—", 0, 0, "—", 0);
        }
        double momentum = QuantMath.momentum(closes, 20);
        double rsi = QuantMath.rsi(closes, 14);
        double ma50 = QuantMath.movingAverage(closes, Math.min(50, closes.size()));
        double change = QuoteText.dayChange(closes);
        int score = QuantMath.amqsScore(momentum, rsi, closes.get(closes.size() - 1), ma50);
        return new QuantSnapshot.Stock(ticker, name, QuoteText.usd(closes.get(closes.size() - 1)),
                QuoteText.percent(change), QuantMath.round2(momentum), (int) Math.round(rsi),
                QuantMath.momentumSignal(momentum), score);
    }

    private double fearGreed(Map<String, List<PriceBar>> book) {
        double spy = FearGreedMath.return20(Bars.closes(quoteBook.bars(book, "SPY")));
        double sum = 0;
        int count = 0;
        for (String ticker : SECTORS.keySet()) {
            FearGreedMath.Score score = FearGreedMath.score(
                    Bars.closes(quoteBook.bars(book, ticker)),
                    Bars.volumes(quoteBook.bars(book, ticker)),
                    spy);
            if (score != null) {
                sum += score.total();
                count++;
            }
        }
        return count == 0 ? 50 : sum / count;
    }

    private PickRow regimeRow(QuantSnapshot snapshot, Map<String, List<PriceBar>> book) {
        QuantMath.Hedge hedge = snapshot.hedge();
        List<Double> closes = Bars.closes(quoteBook.bars(book, "QQQ"));
        String price = closes.isEmpty() ? "—" : QuoteText.usd(closes.get(closes.size() - 1));
        String change = closes.size() < 2 ? snapshot.regimeLabel() : QuoteText.percent(QuoteText.dayChange(closes));
        String direction = closes.size() < 2 ? "zero" : QuoteText.direction(QuoteText.dayChange(closes));
        String detail = snapshot.regimeLabel() + " · 롱 " + hedge.longWeight() + "% / 헤지 " + hedge.hedge() + "%";
        return row("QQQ", "나스닥100", price, change, direction, "퀀트", "VIX " + snapshot.vix(), detail,
                metrics("QQQ", snapshot.regimeSignal(), "VIX " + QuoteText.number(snapshot.vix()),
                        snapshot.regimeLabel(), "퀀트", "ARDS-X", hedge.longWeight() + "%", hedge.hedge() + "%"));
    }

    private PickRow stockRow(QuantSnapshot.Stock stock) {
        double change = signed(stock.changePct());
        return row(stock.ticker(), stock.name(), stock.price(), stock.changePct(), QuoteText.direction(change),
                "퀀트", "모멘텀 " + stock.momentum(), stock.signal() + " · RSI " + stock.rsi(),
                metrics(stock.price(), stock.signal(), "모멘텀 " + stock.momentum(), "RSI " + stock.rsi(),
                        "퀀트", "AMQS", String.valueOf(stock.score()), "—"));
    }

    private PickRow fearRow(QuantSnapshot snapshot) {
        return row("FG", "섹터 심리", String.format(java.util.Locale.US, "%.1f", snapshot.fearGreed()),
                snapshot.fearGreedStatus(), "zero", "퀀트", "—", "10개 섹터 ETF 공포·탐욕",
                metrics(String.format(java.util.Locale.US, "%.1f", snapshot.fearGreed()), snapshot.fearGreedStatus(),
                        "—", "—", "퀀트", "Fear&Greed", "—", "—"));
    }

    private PickRow hynixRow(HynixPredictMath.Prediction prediction) {
        return row("000660", "SK하이닉스", QuoteText.krw(prediction.hynixPredictedOpen()),
                QuoteText.percent(prediction.hynixPredictedChangePct()),
                QuoteText.direction(prediction.hynixPredictedChangePct()),
                "퀀트", "베타 " + prediction.beta(),
                "MU " + prediction.muSignal() + " → 하이닉스 " + prediction.hynixSignal(),
                metrics(QuoteText.krw(prediction.hynixPrevClose()), prediction.hynixSignal(),
                        "MU " + QuoteText.percent(prediction.muChangePct()), "베타 " + prediction.beta(),
                        "퀀트", "MU-Hynix", "R2 " + prediction.r2(), QuoteText.usd(prediction.muPrice())));
    }

    private void addTape(List<EconomyPickBoard.Tape> tapes, Map<String, List<PriceBar>> book, String symbol,
            String name) {
        List<Double> closes = Bars.closes(quoteBook.bars(book, symbol));
        if (closes.size() < 2) {
            return;
        }
        double change = QuoteText.dayChange(closes);
        String price = QuoteText.usd(closes.get(closes.size() - 1));
        String mark = symbol.startsWith("BTC") ? "BTC" : symbol;
        tapes.add(new EconomyPickBoard.Tape(symbol, name, price, QuoteText.percent(change),
                QuoteText.direction(change), QuoteText.logo(mark)));
    }

    private double vix(Map<String, List<PriceBar>> book) {
        List<Double> closes = Bars.closes(quoteBook.bars(book, "^VIX"));
        return closes.isEmpty() ? 20 : QuantMath.round2(closes.get(closes.size() - 1));
    }

    private double average(List<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }
        int start = Math.max(0, values.size() - 20);
        double sum = 0;
        int count = 0;
        for (int index = start; index < values.size(); index++) {
            sum += values.get(index);
            count++;
        }
        return count == 0 ? 0 : sum / count;
    }

    private String overall(List<QuantSnapshot.Stock> stocks) {
        double sum = 0;
        int count = 0;
        for (QuantSnapshot.Stock stock : stocks) {
            if (!"—".equals(stock.signal())) {
                sum += stock.momentum();
                count++;
            }
        }
        return QuantMath.momentumSignal(count == 0 ? 0 : sum / count);
    }

    private double signed(String percent) {
        if (percent == null || percent.startsWith("—")) {
            return 0;
        }
        try {
            return Double.parseDouble(percent.replace("%", "").replace("+", ""));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private PickRow row(String symbol, String name, String price, String change, String direction, String market,
            String volume, String detail, List<String> metrics) {
        return new PickRow("QUANT", name, symbol, price, change, direction, QuoteText.logo(symbol), market, volume,
                detail, metrics);
    }

    private List<String> metrics(String... values) {
        return List.of(values);
    }

    private static Map<String, String> ordered(String... pairs) {
        Map<String, String> rows = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            rows.put(pairs[index], pairs[index + 1]);
        }
        return Map.copyOf(rows);
    }
}
