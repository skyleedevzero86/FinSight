package com.sleekydz86.finsight.core.news.domain.vo;

import com.sleekydz86.finsight.core.news.domain.News;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class NewsSourceUrls {

    private NewsSourceUrls() {
    }

    public static String normalize(String url) {
        if (url == null) {
            return "";
        }
        String trimmed = url.trim();
        int scheme = trimmed.indexOf("://");
        int host = scheme < 0 ? 0 : scheme + 3;
        int end = trimmed.length();
        while (end > host && trimmed.charAt(end - 1) == '/') {
            end--;
        }
        return lowerSchemeAndHost(trimmed.substring(0, end));
    }

    private static String lowerSchemeAndHost(String url) {
        int scheme = url.indexOf("://");
        if (scheme < 0) {
            return url;
        }
        String lowered = url.substring(0, scheme).toLowerCase(Locale.ROOT) + url.substring(scheme);
        int hostStart = scheme + 3;
        int slash = lowered.indexOf('/', hostStart);
        int hostEnd = slash < 0 ? lowered.length() : slash;
        int at = lowered.lastIndexOf('@', hostEnd - 1);
        if (at >= hostStart) {
            hostStart = at + 1;
        }
        return lowered.substring(0, hostStart)
                + lowered.substring(hostStart, hostEnd).toLowerCase(Locale.ROOT)
                + lowered.substring(hostEnd);
    }

    public static List<News> withoutStoredDuplicates(List<News> incoming, Set<String> storedUrls) {
        List<News> fresh = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (incoming == null) {
            return fresh;
        }
        for (News news : incoming) {
            if (news == null || news.getNewsMeta() == null) {
                continue;
            }
            String url = normalize(news.getNewsMeta().getSourceUrl());
            news.getNewsMeta().setSourceUrl(url);
            if (url.isBlank() || !seen.add(url) || storedUrls.contains(url)) {
                continue;
            }
            fresh.add(news);
        }
        return fresh;
    }
}
