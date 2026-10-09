package com.sleekydz86.finsight.core.portfolio.service;

import java.util.Locale;

public final class PortfolioShareText {

    private PortfolioShareText() {
    }

    public static String maskName(String name) {
        String trimmed = name == null ? "" : name.trim();
        int[] points = trimmed.codePoints().toArray();
        if (points.length == 0) {
            return "회*";
        }
        StringBuilder masked = new StringBuilder();
        masked.appendCodePoint(points[0]);
        for (int index = 1; index < points.length; index++) {
            masked.append('*');
        }
        return masked.toString();
    }

    public static String amountLabel(String mode, long netWorth) {
        if ("RATIO".equals(mode)) {
            return "비율만";
        }
        if ("EXACT".equals(mode)) {
            return String.format(Locale.US, "%,d원", netWorth);
        }
        return band(netWorth);
    }

    public static int progress(int goalPercentTenths) {
        int percent = goalPercentTenths / 10;
        if (percent < 0) {
            return 0;
        }
        return Math.min(percent, 100);
    }

    public static String signedPercent(Long delta, long netWorth) {
        if (delta == null) {
            return null;
        }
        long previous = netWorth - delta;
        if (previous == 0) {
            return null;
        }
        double rate = delta * 100.0 / previous;
        return String.format(Locale.US, "%+.1f%%", rate);
    }

    private static String band(long value) {
        String sign = value < 0 ? "-" : "";
        long abs = Math.abs(value);
        if (abs >= 100_000_000L) {
            long eok = abs / 100_000_000L;
            long cheon = (abs % 100_000_000L) / 10_000_000L;
            return cheon > 0 ? sign + "약 " + eok + "억 " + cheon + ",000만원" : sign + "약 " + eok + "억원";
        }
        if (abs >= 10_000_000L) {
            long man = (abs / 10_000_000L) * 1000L;
            return sign + "약 " + String.format(Locale.US, "%,d", man) + "만원";
        }
        if (abs >= 10_000L) {
            return sign + "약 " + String.format(Locale.US, "%,d", abs / 10_000L) + "만원";
        }
        return sign + String.format(Locale.US, "%,d", abs) + "원";
    }
}
