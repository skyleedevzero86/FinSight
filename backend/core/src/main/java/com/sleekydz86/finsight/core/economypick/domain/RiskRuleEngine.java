package com.sleekydz86.finsight.core.economypick.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class RiskRuleEngine {

    public record Filing(String title, LocalDate filedAt) {
    }

    public record Firing(String ruleName, String label, double score, int matchCount, String detail, LocalDate latest) {
    }

    private enum FilingRule {
        NAME_CHANGE("사명변경", 25, "상호변경|사명변경|명칭변경"),
        MAJOR_HOLDER("대주주변경", 20, "최대주주\\s*변경|대주주"),
        LAWSUIT("소송/분쟁", 25, "소송|분쟁|경영권|가처분|회생|주주제안"),
        CB_ISSUANCE("CB발행", 10, "전환사채|신주인수권|사채"),
        CB_REFIX("CB리픽싱", 15, "전환가액.*조정|리픽싱"),
        CAPITAL("증자/감자", 10, "유상증자|무상증자|감자|주식병합"),
        PAYMENT_DELAY("대금지연", 20, "납입.*지연|잔금.*지연|납입기한.*변경"),
        AUDIT_RISK("감사위험", 30, "감사의견.*거절|감사의견.*한정|감사인.*변경"),
        DELIST_RISK("상장폐지위험", 35, "상장폐지|거래정지|관리종목"),
        EMBEZZLE("횡령/배임", 40, "횡령|배임|자금유용");

        private final String label;
        private final int weight;
        private final Pattern pattern;

        FilingRule(String label, int weight, String pattern) {
            this.label = label;
            this.weight = weight;
            this.pattern = Pattern.compile(pattern);
        }
    }

    private RiskRuleEngine() {
    }

    public static List<Firing> evaluateFilings(List<Filing> filings, LocalDate asOf, int windowDays) {
        LocalDate cutoff = asOf.minusDays(windowDays);
        List<Firing> results = new ArrayList<>();
        for (FilingRule rule : FilingRule.values()) {
            Firing firing = match(rule, filings, cutoff);
            if (firing != null) {
                results.add(firing);
            }
        }
        return results;
    }

    public static List<Firing> composite(List<Firing> filings, List<Firing> relations, LocalDate asOf) {
        List<Firing> all = new ArrayList<>();
        if (filings != null) {
            all.addAll(filings);
        }
        if (relations != null) {
            all.addAll(relations);
        }
        if (all.size() < 3) {
            return List.of();
        }
        double average = 0;
        StringBuilder labels = new StringBuilder();
        for (Firing firing : all) {
            average += firing.score();
            if (!labels.isEmpty()) {
                labels.append(", ");
            }
            labels.append(firing.label());
        }
        double score = Math.min(average / all.size(), 1);
        String detail = all.size() + "개 룰 동시 발화: " + labels;
        return List.of(new Firing("CLUSTER_SIGNAL", "복합위험신호", score, all.size(), detail, asOf));
    }

    public static int totalRisk(List<Firing> results) {
        if (results == null || results.isEmpty()) {
            return 0;
        }
        double sum = 0;
        for (Firing firing : results) {
            sum += firing.score() * 100;
        }
        return Math.min((int) Math.round(sum), 100);
    }

    private static Firing match(FilingRule rule, List<Filing> filings, LocalDate cutoff) {
        int count = 0;
        LocalDate latest = null;
        if (filings == null) {
            return null;
        }
        for (Filing filing : filings) {
            if (filing == null || filing.filedAt() == null || filing.filedAt().isBefore(cutoff)) {
                continue;
            }
            if (filing.title() != null && rule.pattern.matcher(filing.title()).find()) {
                count++;
                latest = latest == null || filing.filedAt().isAfter(latest) ? filing.filedAt() : latest;
            }
        }
        if (count == 0) {
            return null;
        }
        double score = Math.min(rule.weight * Math.min(count, 3), 100) / 100.0;
        String detail = count + "건 (최근: " + latest + ")";
        return new Firing(rule.name(), rule.label, score, count, detail, latest);
    }
}
