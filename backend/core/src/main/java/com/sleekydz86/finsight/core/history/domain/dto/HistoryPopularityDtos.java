package com.sleekydz86.finsight.core.history.domain.dto;

import java.util.List;

public final class HistoryPopularityDtos {
    private HistoryPopularityDtos() {
    }

    public record PopularityRequest(List<PopularityItem> items) {
        public PopularityRequest {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }

    public record PopularityItem(String key, String kind, String id) {
    }

    public record PopularityResponse(List<PopularityScore> scores) {
        public PopularityResponse {
            scores = scores == null ? List.of() : List.copyOf(scores);
        }
    }

    public record PopularityScore(String key, long score) {
    }
}
