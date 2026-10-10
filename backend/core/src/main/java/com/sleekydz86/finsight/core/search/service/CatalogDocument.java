package com.sleekydz86.finsight.core.search.service;

public record CatalogDocument(
        String sourceType,
        String sourceId,
        String title,
        String body,
        String href
) {
}
