package com.sleekydz86.finsight.core.portfolio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioAssetJpaEntity;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioAssetCategory;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioSummary;
import com.sleekydz86.finsight.core.portfolio.domain.port.in.dto.PortfolioShareCommand;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class PortfolioShareSnapshots {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy년 M월");
    private static final int MAX_ASSETS = 12;
    private static final int MAX_HISTORY = 6;

    private PortfolioShareSnapshots() {
    }

    public static String write(
            PortfolioShareCommand command,
            PortfolioSummary summary,
            List<PortfolioAssetJpaEntity> assets,
            YearMonth month) {
        String mode = command.amountMode();
        ObjectNode root = MAPPER.createObjectNode();
        root.put("exactNames", command.showExactNames());
        root.put("principal", command.showPrincipal());
        root.put("profit", command.showProfit());
        root.put("totalAssets", assetsLabel(command, mode, summary.totalAssets()));
        root.put("totalLiabilities", debtLabel(command, mode, summary.totalLiabilities()));
        root.put("netWorth", netLabel(command, mode, summary.netWorth()));
        root.set("assets", assetLines(command, mode, assets));
        root.set("history", historyLines(command, mode, summary.netWorth(), summary.history(), month));
        return root.toString();
    }

    private static ArrayNode assetLines(PortfolioShareCommand command, String mode, List<PortfolioAssetJpaEntity> assets) {
        ArrayNode lines = MAPPER.createArrayNode();
        if (assets == null) {
            return lines;
        }
        int kept = 0;
        for (PortfolioAssetJpaEntity asset : assets) {
            if (kept >= MAX_ASSETS || !visible(command, asset.getAssetCategory())) {
                continue;
            }
            lines.add(assetLine(command, mode, asset));
            kept++;
        }
        return lines;
    }

    private static boolean visible(PortfolioShareCommand command, PortfolioAssetCategory category) {
        if (category == PortfolioAssetCategory.LIABILITY) {
            return command.showDebt();
        }
        return command.showAllocation();
    }

    private static ObjectNode assetLine(PortfolioShareCommand command, String mode, PortfolioAssetJpaEntity asset) {
        ObjectNode line = MAPPER.createObjectNode();
        PortfolioAssetCategory category = asset.getAssetCategory();
        line.put("category", category == null ? "기타" : category.label());
        line.put("name", command.showExactNames() ? clip(asset.getAssetName()) : "");
        line.put("amount", PortfolioShareText.amountLabel(mode, asset.getValuationAmount()));
        line.put("acquisition", acquisition(command, mode, asset.getAcquisitionAmount()));
        line.put("quantity", command.showExactNames() ? quantity(asset.getQuantity()) : "");
        line.put("unitPrice", command.showExactNames() && asset.getUnitPrice() != null
                ? PortfolioShareText.amountLabel(mode, asset.getUnitPrice()) : "");
        line.put("profit", profit(command, mode, asset));
        return line;
    }

    private static ArrayNode historyLines(
            PortfolioShareCommand command,
            String mode,
            long netWorth,
            List<PortfolioSummary.HistoryRow> history,
            YearMonth month) {
        ArrayNode lines = MAPPER.createArrayNode();
        if (history == null || history.isEmpty()) {
            lines.add(historyLine(monthLabel(month), netLabel(command, mode, netWorth), "-", "확정"));
            return lines;
        }
        int start = Math.max(0, history.size() - MAX_HISTORY);
        for (int index = start; index < history.size(); index++) {
            PortfolioSummary.HistoryRow row = history.get(index);
            lines.add(historyLine(
                    monthText(row.yearMonth(), month),
                    netLabel(command, mode, row.netWorth()),
                    delta(command, row),
                    statusLabel(row.status())));
        }
        return lines;
    }

    private static ObjectNode historyLine(String month, String netWorth, String delta, String status) {
        ObjectNode line = MAPPER.createObjectNode();
        line.put("month", month == null ? "" : month);
        line.put("netWorth", netWorth);
        line.put("delta", delta);
        line.put("status", status);
        return line;
    }

    private static String assetsLabel(PortfolioShareCommand command, String mode, long amount) {
        if (!command.showAllocation() && !command.showNetWorth()) {
            return "비공개";
        }
        return PortfolioShareText.amountLabel(mode, amount);
    }

    private static String debtLabel(PortfolioShareCommand command, String mode, long amount) {
        if (!command.showDebt()) {
            return "비공개";
        }
        return PortfolioShareText.amountLabel(mode, amount);
    }

    private static String netLabel(PortfolioShareCommand command, String mode, long amount) {
        if (!command.showNetWorth() || "RATIO".equals(mode)) {
            return "비율만";
        }
        return PortfolioShareText.amountLabel(mode, amount);
    }

    private static String acquisition(PortfolioShareCommand command, String mode, Long amount) {
        if (!command.showPrincipal() || amount == null) {
            return "";
        }
        return PortfolioShareText.amountLabel(mode, amount);
    }

    private static String profit(PortfolioShareCommand command, String mode, PortfolioAssetJpaEntity asset) {
        if (!command.showProfit() || asset.getAcquisitionAmount() == null) {
            return "";
        }
        return PortfolioShareText.amountLabel(mode, asset.getValuationAmount() - asset.getAcquisitionAmount());
    }

    private static String delta(PortfolioShareCommand command, PortfolioSummary.HistoryRow row) {
        if (!command.showMonthRate()) {
            return "-";
        }
        String signed = PortfolioShareText.signedPercent(row.delta(), row.netWorth());
        return signed == null ? "-" : signed;
    }

    private static String quantity(BigDecimal quantity) {
        if (quantity == null) {
            return "";
        }
        return quantity.stripTrailingZeros().toPlainString();
    }

    private static String clip(String name) {
        String trimmed = name == null ? "" : name.trim();
        return trimmed.length() <= 80 ? trimmed : trimmed.substring(0, 80);
    }

    private static String monthLabel(YearMonth month) {
        return month == null ? "" : MONTH.format(month);
    }

    private static String monthText(String raw, YearMonth fallback) {
        if (raw == null || raw.isBlank()) {
            return monthLabel(fallback);
        }
        try {
            return MONTH.format(YearMonth.parse(raw));
        } catch (DateTimeParseException exception) {
            return raw;
        }
    }

    private static String statusLabel(String status) {
        if (status == null || status.isBlank() || "CONFIRMED".equals(status)) {
            return "확정";
        }
        return status;
    }
}
