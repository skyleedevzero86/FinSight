package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.global.annotation.CurrentUser;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.AuthenticatedUser;
import com.sleekydz86.finsight.core.global.exception.SystemException;
import com.sleekydz86.finsight.core.global.exception.ValidationException;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioShareFeed;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioSummary;
import com.sleekydz86.finsight.core.portfolio.domain.port.in.dto.PortfolioAssetCommand;
import com.sleekydz86.finsight.core.portfolio.domain.port.in.dto.PortfolioShareCommand;
import com.sleekydz86.finsight.core.portfolio.service.PortfolioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portfolio")
public class PortfolioController {

    private static final Logger log = LoggerFactory.getLogger(PortfolioController.class);

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PortfolioSummary>> dashboard(@CurrentUser AuthenticatedUser currentUser) {
        return ok(load(() -> portfolioService.dashboard(currentUser.getId())), "포트폴리오를 조회했습니다");
    }

    @PostMapping("/assets")
    public ResponseEntity<ApiResponse<PortfolioSummary>> register(
            @CurrentUser AuthenticatedUser currentUser,
            @RequestBody PortfolioAssetCommand command) {
        return ok(load(() -> portfolioService.register(currentUser.getId(), command)), "자산을 등록했습니다");
    }

    @DeleteMapping("/assets/{assetId}")
    public ResponseEntity<ApiResponse<PortfolioSummary>> delete(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable Long assetId) {
        return ok(load(() -> portfolioService.delete(currentUser.getId(), assetId)), "자산을 삭제했습니다");
    }

    @GetMapping("/shares")
    public ResponseEntity<ApiResponse<PortfolioShareFeed>> shares(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "2") int size) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    portfolioService.listPublicShares(page, size),
                    "포트폴리오 공유를 조회했습니다"));
        } catch (RuntimeException exception) {
            log.error("포트폴리오 공유 목록 조회 실패", exception);
            throw new SystemException("포트폴리오 공유를 불러오지 못했습니다.", "PORTFOLIO_SHARE_LIST_ERROR", exception);
        }
    }

    @PostMapping("/shares")
    public ResponseEntity<ApiResponse<PortfolioSummary>> publish(
            @CurrentUser AuthenticatedUser currentUser,
            @RequestBody PortfolioShareCommand command) {
        return ok(load(() -> portfolioService.publish(currentUser.getId(), command)), "포트폴리오를 공유했습니다");
    }

    @PostMapping("/snapshots")
    public ResponseEntity<ApiResponse<PortfolioSummary>> record(
            @CurrentUser AuthenticatedUser currentUser,
            @RequestBody(required = false) SnapshotNote note) {
        String memo = note == null ? null : note.memo();
        return ok(load(() -> portfolioService.recordCurrentMonth(currentUser.getId(), memo)), "이번 달 기록을 저장했습니다");
    }

    private PortfolioSummary load(Loader loader) {
        try {
            return loader.load();
        } catch (ValidationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("포트폴리오 처리 실패", exception);
            throw new SystemException("포트폴리오 처리 중 오류가 발생했습니다.", "PORTFOLIO_ERROR", exception);
        }
    }

    private ResponseEntity<ApiResponse<PortfolioSummary>> ok(PortfolioSummary summary, String message) {
        return ResponseEntity.ok(ApiResponse.success(summary, message));
    }

    @FunctionalInterface
    private interface Loader {
        PortfolioSummary load();
    }

    public record SnapshotNote(String memo) {
    }
}
