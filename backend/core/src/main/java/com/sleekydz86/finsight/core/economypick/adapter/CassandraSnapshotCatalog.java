package com.sleekydz86.finsight.core.economypick.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.economypick.domain.RiskRuleEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CassandraSnapshotCatalog {

    private static final Logger log = LoggerFactory.getLogger(CassandraSnapshotCatalog.class);
    private static final DateTimeFormatter BASIC = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String[] EVENT_FILES = {
            "dart-nameChanges-12m.json",
            "dart-majorHolderChanges-12m.json",
            "dart-lawsuits-12m.json",
            "dart-purposeAdditions-12m.json"
    };

    private final Map<String, Company> companies;
    private final List<Anomaly> movers;

    public CassandraSnapshotCatalog(ObjectMapper objectMapper) {
        this.companies = loadEvents(objectMapper);
        this.movers = loadMovers(objectMapper);
    }

    public Map<String, Company> companies() {
        return companies;
    }

    public List<Anomaly> movers() {
        return movers;
    }

    private Map<String, Company> loadEvents(ObjectMapper objectMapper) {
        Map<String, Company> rows = new LinkedHashMap<>();
        for (String file : EVENT_FILES) {
            for (JsonNode node : array(objectMapper, file, "data")) {
                addFiling(rows, node);
            }
        }
        return Map.copyOf(rows);
    }

    private void addFiling(Map<String, Company> rows, JsonNode node) {
        String code = text(node, "stockCode");
        String title = text(node, "reportName");
        LocalDate filedAt = date(text(node, "date"));
        if (code.isBlank() || title.isBlank() || filedAt == null) {
            return;
        }
        Company company = rows.computeIfAbsent(code, key -> new Company(text(node, "companyName"), key, new ArrayList<>()));
        company.filings().add(new RiskRuleEngine.Filing(title.trim(), filedAt));
    }

    private List<Anomaly> loadMovers(ObjectMapper objectMapper) {
        List<Anomaly> rows = new ArrayList<>();
        for (JsonNode node : array(objectMapper, "kosdaq-anomaly-report.json", "stocks")) {
            Anomaly anomaly = anomaly(node);
            if (anomaly != null) {
                rows.add(anomaly);
            }
        }
        rows.sort((left, right) -> Double.compare(Math.abs(right.changePercent()), Math.abs(left.changePercent())));
        return List.copyOf(rows);
    }

    private Anomaly anomaly(JsonNode node) {
        String code = text(node, "code");
        if (code.isBlank()) {
            return null;
        }
        double change = node.path("changePercent").asDouble(0);
        return new Anomaly(text(node, "name"), code, text(node, "price"), text(node, "change"), change,
                text(node, "volume"), text(node, "marketCap"), node.path("rank").asInt(0));
    }

    private List<JsonNode> array(ObjectMapper objectMapper, String file, String field) {
        try (InputStream input = getClass().getResourceAsStream("/economypick/" + file)) {
            if (input == null) {
                log.warn("카산드라 시세 파일이 없습니다. file={}", file);
                return List.of();
            }
            List<JsonNode> rows = new ArrayList<>();
            objectMapper.readTree(input).path(field).forEach(rows::add);
            return rows;
        } catch (Exception exception) {
            log.warn("카산드라 시세 파일을 읽지 못했습니다. file={}", file, exception);
            return List.of();
        }
    }

    private LocalDate date(String value) {
        if (value == null || value.length() != 8) {
            return null;
        }
        try {
            return LocalDate.parse(value, BASIC);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("").trim();
    }

    public record Company(String name, String code, List<RiskRuleEngine.Filing> filings) {
    }

    public record Anomaly(String name, String code, String price, String change, double changePercent, String volume,
            String marketCap, int rank) {
    }
}
