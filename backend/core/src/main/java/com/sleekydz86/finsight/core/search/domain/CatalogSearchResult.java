package com.sleekydz86.finsight.core.search.domain;

import java.util.List;

public record CatalogSearchResult(
        String query,
        long total,
        long tookMs,
        String engine,
        boolean cached,
        List<SearchSection> sections
) {
    public record SearchSection(String key, String label, long total, String note, List<SearchHit> hits) {
    }

    public record SearchHit(String title, String snippet, String href) {
    }
}
