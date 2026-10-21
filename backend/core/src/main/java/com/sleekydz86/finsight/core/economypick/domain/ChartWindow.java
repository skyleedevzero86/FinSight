package com.sleekydz86.finsight.core.economypick.domain;

public record ChartWindow(String range, String interval, String label) {

    public static ChartWindow of(String period) {
        String key = period == null ? "" : period.trim();
        return switch (key) {
            case "3분" -> new ChartWindow("1d", "2m", "2분");
            case "15분" -> new ChartWindow("5d", "15m", "15분");
            case "주" -> new ChartWindow("1y", "1wk", "주");
            case "월" -> new ChartWindow("5y", "1mo", "월");
            default -> new ChartWindow("3mo", "1d", "일");
        };
    }

    public static String yahooSymbol(String symbol, String market) {
        String cleaned = symbol == null ? "" : symbol.trim();
        if (!cleaned.matches("\\d{6}")) {
            return cleaned;
        }
        String suffix = market != null && market.contains("코스닥") ? ".KQ" : ".KS";
        return cleaned + suffix;
    }
}
