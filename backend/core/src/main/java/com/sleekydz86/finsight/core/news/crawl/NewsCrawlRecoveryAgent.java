package com.sleekydz86.finsight.core.news.crawl;

import com.sleekydz86.finsight.core.global.security.SecurityAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NewsCrawlRecoveryAgent {

    private static final Logger log = LoggerFactory.getLogger(NewsCrawlRecoveryAgent.class);
    private static final String MODEL_FALLBACK = "모델 호출에 실패해 규칙 분류만 유지합니다.";

    private final NewsCrawlIncidentService incidents;
    private final NewsProviderRecrawl recrawl;
    private final NewsCrawlNarrative narrative;

    public NewsCrawlRecoveryAgent(
            NewsCrawlIncidentService incidents,
            NewsProviderRecrawl recrawl,
            NewsCrawlNarrative narrative) {
        this.incidents = incidents;
        this.recrawl = recrawl;
        this.narrative = narrative;
    }

    public List<NewsCrawlIncident> listIncidents(int limit) {
        return incidents.list(limit);
    }

    public NewsCrawlIncident incident(long incidentId) {
        return incidents.get(incidentId);
    }

    public List<NewsCrawlAttempt> attempts(long incidentId) {
        return incidents.attempts(incidentId);
    }

    public NewsCrawlReport analyze(long incidentId) {
        NewsCrawlIncident incident = incidents.get(incidentId);
        String note = explain(incident);
        NewsCrawlIncident analyzed = incidents.markAnalyzed(incidentId, note);
        return incidents.report(analyzed, null, analyzed.retryable()
                ? "규칙 분류와 재처리 정책을 확인했습니다. 관리자 승인이 필요합니다."
                : "재처리 정책이 자동 재수집을 허용하지 않습니다.");
    }

    public NewsCrawlIncident approve(long incidentId, String approver) {
        SecurityAccess.requireAdminOrManager();
        return incidents.approve(incidentId, approver);
    }

    public NewsCrawlReport execute(long incidentId, String executor) {
        SecurityAccess.requireAdminOrManager();
        return recrawl.executeApproved(incidentId, executor);
    }

    private String explain(NewsCrawlIncident incident) {
        try {
            String note = narrative.explain(incident);
            if (note == null || note.isBlank()) {
                return MODEL_FALLBACK;
            }
            return note;
        } catch (Exception error) {
            log.warn("뉴스 수집 장애 모델 분석 실패 incidentId={}", incident.id(), error);
            return MODEL_FALLBACK;
        }
    }
}
