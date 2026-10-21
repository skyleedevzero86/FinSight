package com.sleekydz86.finsight.core.news.domain.port.in.dto;

import java.util.List;

public record EconomyPickNewsView(String mode, List<Item> items) {

    public record Item(
            long id,
            String title,
            String source,
            String publishedAt,
            String href,
            String imageUrl) {
    }
}
