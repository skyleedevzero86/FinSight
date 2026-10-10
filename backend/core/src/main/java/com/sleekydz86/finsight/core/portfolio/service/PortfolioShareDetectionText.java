package com.sleekydz86.finsight.core.portfolio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public final class PortfolioShareDetectionText {

    private PortfolioShareDetectionText() {
    }

    public static boolean merge(String raw, Set<Long> shareIds, Map<String, Set<Long>> hits, ObjectMapper mapper) {
        if (raw == null || raw.isBlank() || mapper == null || shareIds == null) {
            return false;
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return false;
        }
        try {
            JsonNode root = mapper.readTree(raw.substring(start, end + 1));
            JsonNode rows = root.get("hits");
            if (rows == null || !rows.isArray()) {
                return false;
            }
            boolean accepted = false;
            for (JsonNode row : rows) {
                long id = row.path("id").asLong(0);
                if (!shareIds.contains(id)) {
                    continue;
                }
                for (JsonNode label : row.path("labels")) {
                    Set<Long> bucket = hits.get(label.asText(""));
                    if (bucket != null) {
                        bucket.add(id);
                        accepted = true;
                    }
                }
            }
            return accepted;
        } catch (IOException exception) {
            return false;
        }
    }
}
