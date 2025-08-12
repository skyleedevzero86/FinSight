package com.sleekydz86.finsight.core.news.domain.vo;

import java.util.Arrays;
import java.util.List;

public enum NewsProvider {
    ALL,
    BLOOMBERG;

    public static List<NewsProvider> getAllProviders() {
        return Arrays.asList(values());
    }
}