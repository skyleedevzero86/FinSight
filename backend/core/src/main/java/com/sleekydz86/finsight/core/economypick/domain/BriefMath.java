package com.sleekydz86.finsight.core.economypick.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.economypick.domain.EconomyPickSales.Slice;

import java.util.ArrayList;
import java.util.List;

public final class BriefMath {

    private BriefMath() {
    }

    public static EconomyPickBrief rules(String name, String symbol) {
        EconomyPickSales sales = SalesMixMath.predict(name, symbol);
        List<String> points = new ArrayList<>();
        for (Slice slice : sales.slices()) {
            points.add(slice.label() + " " + trim(slice.percent()) + "%");
        }
        return new EconomyPickBrief("RULE", sales.note(), "야후 시세와 섹터 규칙으로 나눈 추정입니다.", points);
    }

    public static EconomyPickBrief ollama(String raw, ObjectMapper mapper) {
        JsonNode node = json(raw, mapper);
        if (node == null) {
            return null;
        }
        String headline = clip(node.path("headline").asText(""), 80);
        String body = clip(node.path("body").asText(""), 180);
        List<String> points = points(node.path("points"));
        if (headline.isBlank() || body.isBlank() || points.isEmpty()) {
            return null;
        }
        return new EconomyPickBrief("OLLAMA", headline, body, points);
    }

    private static JsonNode json(String raw, ObjectMapper mapper) {
        if (raw == null || mapper == null) {
            return null;
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        try {
            return mapper.readTree(raw.substring(start, end + 1));
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private static List<String> points(JsonNode node) {
        List<String> rows = new ArrayList<>();
        if (!node.isArray()) {
            return rows;
        }
        for (JsonNode item : node) {
            String text = clip(item.asText(""), 80);
            if (!text.isBlank() && rows.size() < 4) {
                rows.add(text);
            }
        }
        return rows;
    }

    private static String clip(String value, int max) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private static String trim(double value) {
        return value == Math.rint(value) ? String.valueOf((int) value) : String.valueOf(value);
    }
}
