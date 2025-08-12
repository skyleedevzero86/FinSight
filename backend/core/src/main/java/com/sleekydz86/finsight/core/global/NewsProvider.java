package com.sleekydz86.finsight.core.global;

import java.util.Arrays;
import java.util.List;

public enum NewsProvider {
    ALL,
    BLOOMBERG,
    MARKETAUX;

    public static List<NewsProvider> getAllProviders() {
        return Arrays.asList(values());
    }
}