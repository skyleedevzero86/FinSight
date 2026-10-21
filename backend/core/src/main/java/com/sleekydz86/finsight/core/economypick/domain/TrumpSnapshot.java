package com.sleekydz86.finsight.core.economypick.domain;

import java.util.List;

public record TrumpSnapshot(
        String source,
        String summary,
        String mood,
        String marketImpact,
        List<String> topics,
        List<String> risks,
        List<TrumpSectorBook.Pick> picks,
        List<Item> items) {

    public record Item(String title, String source, String link, String type) {
    }
}
