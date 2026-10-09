package com.sleekydz86.finsight.core.news.domain.vo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NewsTextRepair {

    private static final Pattern UNICODE = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
    private static final Pattern SURROGATE = Pattern.compile("[\\uD800-\\uDFFF]");

    private NewsTextRepair() {
    }

    public static String repair(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String folded = value.replace("\\\\", "\\");
        String marked = folded.replaceAll("(?i)S\\\\udc5e0?(?=\\s*500)", "S&P");
        return SURROGATE.matcher(decodeUnicode(marked)).replaceAll("");
    }

    private static String decodeUnicode(String text) {
        Matcher matcher = UNICODE.matcher(text);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(decoded(matcher.group(1))));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String decoded(String hex) {
        int code = Integer.parseInt(hex, 16);
        if (code >= 0xD800 && code <= 0xDFFF) {
            return "";
        }
        return String.valueOf((char) code);
    }
}
