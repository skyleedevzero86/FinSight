package com.sleekydz86.finsight.core.portfolio.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareJpaEntity;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioShareView;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioShareView.AssetLine;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioShareView.HistoryLine;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class PortfolioShareViews {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy년 M월");

    private PortfolioShareViews() {
    }

    public static PortfolioShareView from(PortfolioShareJpaEntity share) {
        String amount = share.getAmountLabel() == null ? "" : share.getAmountLabel().trim();
        String monthRate = blankToNull(share.getMonthRateLabel());
        String goalRate = blankToNull(share.getGoalRateLabel());
        boolean showNetWorth = !amount.isBlank();
        String recordMonth = share.getCreatedAt() == null ? "" : MONTH.format(share.getCreatedAt());
        String netWorth = amount.isBlank() ? "비율만" : amount;
        JsonNode detail = read(share.getPublicDetail());
        Flags flags = flags(detail);
        return new PortfolioShareView(
                showNetWorth,
                monthRate != null,
                share.isShowAsset(),
                goalRate != null,
                flags.exactNames,
                share.isShowDebt(),
                flags.principal,
                flags.profit,
                amountMode(amount),
                share.getVisibility() == null ? "PUBLIC" : share.getVisibility(),
                share.getMessage() == null ? "" : share.getMessage(),
                share.getGoalLabel() == null ? "1억 만들기" : share.getGoalLabel(),
                netWorth,
                share.getProgressPercent(),
                monthRate,
                recordMonth,
                text(detail, "totalAssets", share.isShowAsset() ? netWorth : "비공개"),
                text(detail, "totalLiabilities", share.isShowDebt() ? "공개" : "비공개"),
                text(detail, "netWorth", netWorth),
                assets(detail, share.isShowAsset(), netWorth),
                history(detail, recordMonth, netWorth, monthRate));
    }

    private static List<AssetLine> assets(JsonNode detail, boolean showAsset, String netWorth) {
        JsonNode rows = detail == null ? null : detail.get("assets");
        if (rows != null && rows.isArray() && !rows.isEmpty()) {
            return assetRows(rows);
        }
        if (!showAsset) {
            return List.of();
        }
        return List.of(new AssetLine("자산 구성", "", netWorth, "", "", "", ""));
    }

    private static List<AssetLine> assetRows(JsonNode rows) {
        List<AssetLine> lines = new ArrayList<>();
        for (JsonNode row : rows) {
            lines.add(new AssetLine(
                    text(row, "category", "기타"),
                    text(row, "name", ""),
                    text(row, "amount", "비율만"),
                    text(row, "acquisition", ""),
                    text(row, "quantity", ""),
                    text(row, "unitPrice", ""),
                    text(row, "profit", "")));
        }
        return List.copyOf(lines);
    }

    private static List<HistoryLine> history(JsonNode detail, String recordMonth, String netWorth, String monthRate) {
        JsonNode rows = detail == null ? null : detail.get("history");
        if (rows != null && rows.isArray() && !rows.isEmpty()) {
            return historyRows(rows);
        }
        String delta = monthRate == null ? "-" : monthRate;
        return List.of(new HistoryLine(recordMonth, netWorth, delta, "확정"));
    }

    private static List<HistoryLine> historyRows(JsonNode rows) {
        List<HistoryLine> lines = new ArrayList<>();
        for (JsonNode row : rows) {
            lines.add(new HistoryLine(
                    text(row, "month", ""),
                    text(row, "netWorth", "비율만"),
                    text(row, "delta", "-"),
                    statusLabel(text(row, "status", "확정"))));
        }
        return List.copyOf(lines);
    }

    private static Flags flags(JsonNode detail) {
        if (detail == null) {
            return new Flags(false, false, false);
        }
        return new Flags(
                detail.path("exactNames").asBoolean(false),
                detail.path("principal").asBoolean(false),
                detail.path("profit").asBoolean(false));
    }

    private static String statusLabel(String status) {
        if ("CONFIRMED".equals(status)) {
            return "확정";
        }
        return status;
    }

    private static JsonNode read(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            JsonNode root = MAPPER.readTree(raw);
            return root != null && root.isObject() ? root : null;
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private static String text(JsonNode node, String field, String fallback) {
        if (node == null || !node.hasNonNull(field)) {
            return fallback;
        }
        String value = node.get(field).asText("").trim();
        return value.isBlank() ? fallback : value;
    }

    private static String amountMode(String amount) {
        if (amount.isBlank() || amount.contains("비율")) {
            return "RATIO";
        }
        if (amount.contains("약") || amount.contains("만원")) {
            return "BAND";
        }
        return "EXACT";
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record Flags(boolean exactNames, boolean principal, boolean profit) {
    }
}
