package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.global.annotation.CurrentUser;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.AuthenticatedUser;
import com.sleekydz86.finsight.core.global.exception.InsufficientPermissionException;
import com.sleekydz86.finsight.core.global.exception.SystemException;
import com.sleekydz86.finsight.core.global.exception.ValidationException;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioDiagnosis;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioShareDetail;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioShareFeed;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioSummary;
import com.sleekydz86.finsight.core.portfolio.domain.port.in.dto.PortfolioAssetCommand;
import com.sleekydz86.finsight.core.portfolio.domain.port.in.dto.PortfolioShareCommand;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioShareCommentItem;
import com.sleekydz86.finsight.core.portfolio.service.PortfolioDiagnosisService;
import com.sleekydz86.finsight.core.portfolio.service.PortfolioService;
import com.sleekydz86.finsight.core.portfolio.service.PortfolioShareCommentService;
import com.sleekydz86.finsight.core.portfolio.service.PortfolioShareFeedbackService;
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
    private final PortfolioDiagnosisService portfolioDiagnosisService;
    private final PortfolioShareFeedbackService feedbackService;
    private final PortfolioShareCommentService commentService;

    public PortfolioController(
            PortfolioService portfolioService,
            PortfolioDiagnosisService portfolioDiagnosisService,
            PortfolioShareFeedbackService feedbackService,
            PortfolioShareCommentService commentService) {
        this.portfolioService = portfolioService;
        this.portfolioDiagnosisService = portfolioDiagnosisService;
        this.feedbackService = feedbackService;
        this.commentService = commentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PortfolioSummary>> dashboard(@CurrentUser AuthenticatedUser currentUser) {
        return ok(load(() -> portfolioService.dashboard(currentUser.getId())), "포트폴리오를 조회했습니다");
    }

    @GetMapping("/diagnosis")
    public ResponseEntity<ApiResponse<PortfolioDiagnosis>> diagnosis(@CurrentUser AuthenticatedUser currentUser) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    portfolioDiagnosisService.diagnose(currentUser.getId()),
                    "자산 진단을 조회했습니다"));
        } catch (ValidationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("자산 진단 실패 userId={}", currentUser.getId(), exception);
            throw new SystemException("자산 진단을 불러오지 못했습니다.", "PORTFOLIO_DIAGNOSIS_ERROR", exception);
        }
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

    @GetMapping("/shares/{shareId}")
    public ResponseEntity<ApiResponse<PortfolioShareDetail>> share(
            @PathVariable Long shareId,
            @CurrentUser(required = false) AuthenticatedUser currentUser) {
        try {
            Long userId = currentUser == null ? null : currentUser.getId();
            return ResponseEntity.ok(ApiResponse.success(feedbackService.detail(shareId, userId), "포트폴리오 공유를 조회했습니다"));
        } catch (ValidationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("포트폴리오 공유 상세 조회 실패 shareId={}", shareId, exception);
            throw new SystemException("포트폴리오 공유를 불러오지 못했습니다.", "PORTFOLIO_SHARE_DETAIL_ERROR", exception);
        }
    }

    @GetMapping("/shares/{shareId}/comments")
    public ResponseEntity<ApiResponse<java.util.List<PortfolioShareCommentItem>>> comments(@PathVariable Long shareId) {
        try {
            return ResponseEntity.ok(ApiResponse.success(commentService.list(shareId), "공유 댓글을 조회했습니다"));
        } catch (ValidationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("포트폴리오 공유 댓글 조회 실패 shareId={}", shareId, exception);
            throw new SystemException("공유 댓글을 불러오지 못했습니다.", "PORTFOLIO_SHARE_COMMENT_LIST_ERROR", exception);
        }
    }

    @PostMapping("/shares/{shareId}/comments")
    public ResponseEntity<ApiResponse<java.util.List<PortfolioShareCommentItem>>> addComment(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable Long shareId,
            @RequestBody CommentRequest request) {
        try {
            String content = request == null ? null : request.content();
            return ResponseEntity.ok(ApiResponse.success(
                    commentService.add(currentUser.getId(), currentUser.getNickname(), shareId, content),
                    "댓글을 등록했습니다"));
        } catch (ValidationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("포트폴리오 공유 댓글 등록 실패 shareId={}", shareId, exception);
            throw new SystemException("댓글을 등록하지 못했습니다.", "PORTFOLIO_SHARE_COMMENT_ERROR", exception);
        }
    }

    @PostMapping("/shares/{shareId}/reactions")
    public ResponseEntity<ApiResponse<PortfolioShareDetail>> react(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable Long shareId,
            @RequestBody ReactionRequest request) {
        try {
            String type = request == null ? null : request.type();
            return ResponseEntity.ok(ApiResponse.success(
                    feedbackService.react(currentUser.getId(), shareId, type),
                    "공유 반응을 저장했습니다"));
        } catch (ValidationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("포트폴리오 공유 반응 실패 shareId={}", shareId, exception);
            throw new SystemException("공유 반응을 저장하지 못했습니다.", "PORTFOLIO_SHARE_REACTION_ERROR", exception);
        }
    }

    @PostMapping("/shares/{shareId}/reports")
    public ResponseEntity<ApiResponse<PortfolioShareDetail>> report(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable Long shareId,
            @RequestBody ReportRequest request) {
        try {
            String reason = request == null ? null : request.reason();
            String note = request == null ? null : request.note();
            return ResponseEntity.ok(ApiResponse.success(
                    feedbackService.report(currentUser.getId(), shareId, reason, note),
                    "공유 게시물을 신고했습니다"));
        } catch (ValidationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("포트폴리오 공유 신고 실패 shareId={}", shareId, exception);
            throw new SystemException("공유 게시물을 신고하지 못했습니다.", "PORTFOLIO_SHARE_REPORT_ERROR", exception);
        }
    }

    @PostMapping("/shares/{shareId}/moderation")
    public ResponseEntity<ApiResponse<PortfolioShareFeed.PortfolioShareCard>> moderate(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable Long shareId,
            @RequestBody ModerationRequest request) {
        try {
            String status = request == null ? null : request.status();
            return ResponseEntity.ok(ApiResponse.success(
                    portfolioService.moderate(currentUser.getRole(), shareId, status),
                    "공유 게시물을 처리했습니다"));
        } catch (ValidationException | InsufficientPermissionException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("포트폴리오 공유 조치 실패 shareId={}", shareId, exception);
            throw new SystemException("공유 게시물을 처리하지 못했습니다.", "PORTFOLIO_SHARE_MODERATION_ERROR", exception);
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

    public record ModerationRequest(String status) {
    }

    public record ReactionRequest(String type) {
    }

    public record ReportRequest(String reason, String note) {
    }

    public record CommentRequest(String content) {
    }
}
