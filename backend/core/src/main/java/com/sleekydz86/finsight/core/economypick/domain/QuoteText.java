package com.sleekydz86.finsight.core.economypick.domain;

import java.util.Locale;
import java.util.Set;

public final class QuoteText {

    private static final Set<String> PLAIN_MARK = Set.of("VIX", "FG");

    private QuoteText() {
    }

    public static String direction(double change) {
        if (change > 0) {
            return "up";
        }
        if (change < 0) {
            return "down";
        }
        return "zero";
    }

    public static String percent(double value) {
        String body = String.format(Locale.US, "%.2f%%", Math.abs(value));
        if (value > 0) {
            return "+" + body;
        }
        if (value < 0) {
            return "-" + body;
        }
        return "0.00%";
    }

    public static String usd(double value) {
        String pattern = Math.abs(value) >= 1000 ? "%,.0f" : "%,.2f";
        return "$" + String.format(Locale.US, pattern, value);
    }

    public static String krw(double value) {
        return String.format(Locale.KOREA, "%,.0f", value);
    }

    public static String logo(String symbol) {
        String ticker = listed(symbol);
        if (ticker.matches("\\d{6}")) {
            return "https://file.alphasquare.co.kr/media/images/stock_logo/kr/" + ticker + ".png";
        }
        if (PLAIN_MARK.contains(ticker)) {
            return "";
        }
        if (ticker.matches("[A-Z][A-Z0-9.\\-]{0,11}")) {
            return "https://assets.parqet.com/logos/symbol/" + ticker;
        }
        return "";
    }

    private static String listed(String symbol) {
        if (symbol == null) {
            return "";
        }
        String cleaned = symbol.trim().toUpperCase(Locale.ROOT);
        if (cleaned.startsWith("^")) {
            return cleaned.substring(1);
        }
        return cleaned;
    }

    public static String number(double value) {
        return String.format(Locale.US, "%,.0f", value);
    }

    public static double dayChange(java.util.List<Double> closes) {
        if (closes == null || closes.size() < 2 || closes.get(closes.size() - 2) == 0) {
            return 0;
        }
        double last = closes.get(closes.size() - 1);
        double previous = closes.get(closes.size() - 2);
        return ((last - previous) / previous) * 100;
    }
}
