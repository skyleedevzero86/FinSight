package com.sleekydz86.finsight.core.portfolio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioDiagnosis;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioSummary.InsightLine;

import java.util.ArrayList;
import java.util.List;

public final class PortfolioDiagnosisText {

    private static final int MAX_LINES = 4;
    private static final int MAX_LINE = 80;
    private static final int MAX_ADVICE = 120;

    private PortfolioDiagnosisText() {
    }

    public static PortfolioDiagnosis parse(String content, ObjectMapper mapper) {
        JsonNode root = readObject(content, mapper);
        if (root == null) {
            return null;
        }
        List<InsightLine> lines = lines(root.get("insights"));
        String advice = clip(text(root.get("advice")), MAX_ADVICE);
        if (lines.isEmpty() || advice.isBlank()) {
            return null;
        }
        return new PortfolioDiagnosis(lines, advice, PortfolioDiagnosis.LLAMA2);
    }

    private static JsonNode readObject(String content, ObjectMapper mapper) {
        String json = objectSlice(content);
        if (json.isBlank()) {
            return null;
        }
        try {
            JsonNode root = mapper.readTree(json);
            return root != null && root.isObject() ? root : null;
        } catch (Exception exception) {
            return null;
        }
    }

    private static String objectSlice(String content) {
        if (content == null) {
            return "";
        }
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return "";
        }
        return content.substring(start, end + 1);
    }

    private static List<InsightLine> lines(JsonNode node) {
        List<InsightLine> lines = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return lines;
        }
        for (JsonNode item : node) {
            if (lines.size() >= MAX_LINES) {
                break;
            }
            InsightLine line = line(item);
            if (line != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static InsightLine line(JsonNode item) {
        if (item == null || !item.isObject()) {
            return null;
        }
        String tone = tone(text(item.get("tone")));
        String text = clip(text(item.get("text")), MAX_LINE);
        if (tone == null || text.isBlank()) {
            return null;
        }
        return new InsightLine(tone, text);
    }

    private static String tone(String raw) {
        if ("GOOD".equals(raw) || "WARN".equals(raw)) {
            return raw;
        }
        return null;
    }

    private static String text(JsonNode node) {
        if (node == null || !node.isTextual()) {
            return "";
        }
        return node.asText("").trim();
    }

    private static String clip(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max).trim();
    }
}
