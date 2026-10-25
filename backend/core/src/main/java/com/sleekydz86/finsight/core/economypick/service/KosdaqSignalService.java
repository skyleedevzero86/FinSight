package com.sleekydz86.finsight.core.economypick.service;

import com.sleekydz86.finsight.core.economypick.adapter.CassandraSnapshotCatalog;
import com.sleekydz86.finsight.core.economypick.adapter.CassandraSnapshotCatalog.Anomaly;
import com.sleekydz86.finsight.core.economypick.adapter.CassandraSnapshotCatalog.Company;
import com.sleekydz86.finsight.core.economypick.domain.KosdaqDashboard;
import com.sleekydz86.finsight.core.economypick.domain.PickRow;
import com.sleekydz86.finsight.core.economypick.domain.QuoteText;
import com.sleekydz86.finsight.core.economypick.domain.RiskRuleEngine;
import com.sleekydz86.finsight.core.economypick.domain.RiskRuleEngine.Firing;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class KosdaqSignalService {

    private final CassandraSnapshotCatalog catalog;

    public KosdaqSignalService(CassandraSnapshotCatalog catalog) {
        this.catalog = catalog;
    }

    public KosdaqDashboard dashboard() {
        List<KosdaqDashboard.Signal> signals = scored(LocalDate.now());
        return new KosdaqDashboard(
                catalog.companies().size(),
                (int) signals.stream().filter(KosdaqDashboard.Signal::highRisk).count(),
                count(signals, "사명변경"),
                count(signals, "대주주변경"),
                count(signals, "소송/분쟁"),
                count(signals, "CB"),
                count(signals, "증자/감자"),
                signals);
    }

    public List<PickRow> rows() {
        List<PickRow> rows = new ArrayList<>();
        for (KosdaqDashboard.Signal signal : dashboard().signals()) {
            if (rows.size() == 8) {
                break;
            }
            rows.add(signalRow(signal));
        }
        for (Anomaly mover : catalog.movers()) {
            if (rows.size() == 8) {
                break;
            }
            if (rows.stream().noneMatch(row -> row.symbol().equals(mover.code()))) {
                rows.add(moverRow(mover));
            }
        }
        return rows;
    }

    private List<KosdaqDashboard.Signal> scored(LocalDate asOf) {
        List<KosdaqDashboard.Signal> rows = new ArrayList<>();
        for (Company company : catalog.companies().values()) {
            KosdaqDashboard.Signal signal = signal(company, asOf);
            if (signal != null) {
                rows.add(signal);
            }
        }
        rows.sort((left, right) -> Integer.compare(right.risk(), left.risk()));
        return rows;
    }

    private KosdaqDashboard.Signal signal(Company company, LocalDate asOf) {
        List<Firing> filings = RiskRuleEngine.evaluateFilings(company.filings(), asOf, 180);
        List<Firing> combined = new ArrayList<>(filings);
        combined.addAll(RiskRuleEngine.composite(filings, List.of(), asOf));
        int risk = RiskRuleEngine.totalRisk(combined);
        if (risk == 0) {
            return null;
        }
        Anomaly quote = quote(company.code());
        String rules = labels(combined);
        String detail = combined.isEmpty() ? "" : combined.get(0).detail();
        return new KosdaqDashboard.Signal(
                name(company, quote),
                company.code(),
                quote == null || quote.price().isBlank() ? "—" : quote.price(),
                quote == null || quote.change().isBlank() ? "위험 " + risk : quote.change(),
                risk,
                risk >= 50,
                rules,
                detail);
    }

    private PickRow signalRow(KosdaqDashboard.Signal signal) {
        Anomaly quote = quote(signal.symbol());
        double change = quote == null ? 0 : quote.changePercent();
        return new PickRow(
                "DASHBOARD",
                signal.name(),
                signal.symbol(),
                signal.price(),
                signal.changePct(),
                QuoteText.direction(change),
                QuoteText.logo(signal.symbol()),
                "코스닥 시그널",
                quote == null ? "—" : quote.volume(),
                signal.rules() + " · 위험 " + signal.risk(),
                companyMetrics(quote));
    }

    private PickRow moverRow(Anomaly mover) {
        return new PickRow(
                "DASHBOARD",
                mover.name(),
                mover.code(),
                blank(mover.price()),
                blank(mover.change()),
                QuoteText.direction(mover.changePercent()),
                QuoteText.logo(mover.code()),
                "코스닥 시그널",
                blank(mover.volume()),
                "고변동 " + QuoteText.percent(mover.changePercent()),
                companyMetrics(mover));
    }

    private Anomaly quote(String code) {
        for (Anomaly mover : catalog.movers()) {
            if (code.equals(mover.code())) {
                return mover;
            }
        }
        return null;
    }

    private String name(Company company, Anomaly quote) {
        if (quote != null && !quote.name().isBlank()) {
            return quote.name();
        }
        return company.name().isBlank() ? company.code() : company.name();
    }

    private String labels(List<Firing> firings) {
        StringBuilder text = new StringBuilder();
        for (Firing firing : firings) {
            if (!text.isEmpty()) {
                text.append(", ");
            }
            text.append(firing.label());
        }
        return text.toString();
    }

    private int count(List<KosdaqDashboard.Signal> signals, String label) {
        return (int) signals.stream().filter(signal -> signal.rules().contains(label)).count();
    }

    private List<String> companyMetrics(Anomaly quote) {
        return List.of(
                quote == null ? "" : filled(quote.marketCap()),
                quote == null || quote.rank() <= 0 ? "" : quote.rank() + "위",
                "",
                "",
                "",
                "",
                "",
                "");
    }

    private String filled(String value) {
        return value == null || value.isBlank() ? "" : value;
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
