package com.sleekydz86.finsight.core.search.service;

public final class SearchSnippets {

    private static final int WINDOW = 90;

    private SearchSnippets() {
    }

    public static String keyword(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.length() > 80) {
            trimmed = trimmed.substring(0, 80);
        }
        return trimmed.replace("%", "").replace("_", "").replace("\\", "");
    }

    public static String snippet(String primary, String fallback, String keyword) {
        String source = firstText(primary, fallback);
        if (source.isBlank()) {
            return "";
        }
        String needle = keyword == null ? "" : keyword.trim();
        int at = needle.isBlank() ? -1 : indexOf(source, needle);
        int start = at < 0 ? 0 : Math.max(0, at - 24);
        int end = Math.min(source.length(), start + WINDOW);
        String slice = source.substring(start, end).replace('\n', ' ').trim();
        if (start > 0) {
            slice = "…" + slice;
        }
        if (end < source.length()) {
            slice = slice + "…";
        }
        return slice;
    }

    public static String title(String primary, String fallback) {
        String value = firstText(primary, fallback);
        return value.isBlank() ? "제목 없음" : clip(value, 120);
    }

    private static String firstText(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        return fallback == null ? "" : fallback.trim();
    }

    private static int indexOf(String source, String needle) {
        return source.toLowerCase().indexOf(needle.toLowerCase());
    }

    private static String clip(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
