package com.sleekydz86.finsight.web.controller.admin;

import com.sleekydz86.finsight.core.global.annotation.SecurityAudit;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.news.crawl.NewsCrawlAttempt;
import com.sleekydz86.finsight.core.news.crawl.NewsCrawlIncident;
import com.sleekydz86.finsight.core.news.crawl.NewsCrawlRecoveryAgent;
import com.sleekydz86.finsight.core.news.crawl.NewsCrawlRecoveryMetrics;
import com.sleekydz86.finsight.core.news.crawl.NewsCrawlReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/news-crawl/incidents")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@Tag(name = "뉴스 수집 장애", description = "뉴스 수집 장애 분석과 승인된 재수집 API (ADMIN/MANAGER)")
@SecurityRequirement(name = "BearerAuth")
public class AdminNewsCrawlController {

    private static final Logger log = LoggerFactory.getLogger(AdminNewsCrawlController.class);

    private final NewsCrawlRecoveryAgent agent;
    private final NewsCrawlRecoveryMetrics metrics;

    public AdminNewsCrawlController(NewsCrawlRecoveryAgent agent, NewsCrawlRecoveryMetrics metrics) {
        this.agent = agent;
        this.metrics = metrics;
    }

    @GetMapping
    @Operation(summary = "수집 장애 목록", description = "최근 뉴스 수집 장애 사건을 조회합니다. 재수집은 실행하지 않습니다.")
    public ResponseEntity<ApiResponse<List<NewsCrawlIncident>>> list(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.success(agent.listIncidents(limit), "뉴스 수집 장애 목록을 조회했습니다"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "수집 장애 상세", description = "사건과 실행 시도를 조회합니다.")
    public ResponseEntity<ApiResponse<IncidentDetail>> detail(@PathVariable long id) {
        IncidentDetail detail = new IncidentDetail(agent.incident(id), agent.attempts(id));
        return ResponseEntity.ok(ApiResponse.success(detail, "뉴스 수집 장애를 조회했습니다"));
    }

    @GetMapping("/observations")
    @Operation(summary = "사건 저장 실패 관측", description = "DB에 남지 못한 장애 관측과 측정 카운터를 반환합니다.")
    public ResponseEntity<ApiResponse<NewsCrawlRecoveryMetrics.Snapshot>> observations() {
        return ResponseEntity.ok(ApiResponse.success(metrics.snapshot(), "뉴스 수집 장애 관측을 조회했습니다"));
    }

    @PostMapping("/{id}/analyze")
    @SecurityAudit(action = "NEWS_CRAWL_ANALYZE", resource = "NEWS_CRAWL", level = SecurityAudit.SecurityLevel.INFO)
    @Operation(summary = "장애 분석", description = "규칙으로 재처리 가능 여부를 정하고 모델 서술을 붙입니다. 재수집은 실행하지 않습니다.")
    public ResponseEntity<ApiResponse<NewsCrawlReport>> analyze(@PathVariable long id) {
        log.info("뉴스 수집 장애 분석 요청 incidentId={}", id);
        return ResponseEntity.ok(ApiResponse.success(agent.analyze(id), "뉴스 수집 장애를 분석했습니다"));
    }

    @PostMapping("/{id}/approve")
    @SecurityAudit(action = "NEWS_CRAWL_APPROVE", resource = "NEWS_CRAWL", level = SecurityAudit.SecurityLevel.WARN)
    @Operation(summary = "재수집 승인", description = "재처리 정책이 허용한 사건만 승인합니다.")
    public ResponseEntity<ApiResponse<NewsCrawlIncident>> approve(@PathVariable long id, Authentication authentication) {
        log.info("뉴스 수집 장애 승인 요청 incidentId={}", id);
        NewsCrawlIncident approved = agent.approve(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(approved, "뉴스 수집 재처리를 승인했습니다"));
    }

    @PostMapping("/{id}/execute")
    @SecurityAudit(action = "NEWS_CRAWL_RECRAWL", resource = "NEWS_CRAWL", level = SecurityAudit.SecurityLevel.WARN)
    @Operation(summary = "승인된 재수집", description = "승인된 사건만 해당 제공처를 다시 수집합니다.")
    public ResponseEntity<ApiResponse<NewsCrawlReport>> execute(@PathVariable long id, Authentication authentication) {
        log.info("뉴스 수집 재처리 요청 incidentId={}", id);
        return ResponseEntity.ok(ApiResponse.success(agent.execute(id, authentication.getName()), "뉴스 수집 재처리를 마쳤습니다"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> illegal(IllegalStateException error) {
        log.warn("뉴스 수집 장애 상태 변경 거부: {}", error.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(error.getMessage(), 409));
    }

    public record IncidentDetail(NewsCrawlIncident incident, List<NewsCrawlAttempt> attempts) {
    }
}
