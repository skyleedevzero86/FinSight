package com.sleekydz86.finsight.web.mcp.tools;

import com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardListResponse;
import com.sleekydz86.finsight.core.board.service.BoardQueryService;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.health.domain.port.in.HealthQueryUseCase;
import com.sleekydz86.finsight.core.health.domain.vo.HealthStatus;
import com.sleekydz86.finsight.core.mainimg.domain.port.in.dto.MainimgItemResponse;
import com.sleekydz86.finsight.core.mainimg.service.MainimgItemService;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.YoutubeMediaAdminUseCase;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeAdminVideoSearchRequest;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeVideoListResponse;
import com.sleekydz86.finsight.core.notification.domain.Notification;
import com.sleekydz86.finsight.core.notification.domain.port.in.NotificationQueryUseCase;
import com.sleekydz86.finsight.core.notification.domain.port.in.dto.EmailLogSearchCriteria;
import com.sleekydz86.finsight.core.notification.domain.port.out.dto.EmailLogResponse;
import com.sleekydz86.finsight.core.notification.service.EmailLogService;
import com.sleekydz86.finsight.core.popup.domain.port.in.dto.PopupItemResponse;
import com.sleekydz86.finsight.core.popup.service.PopupItemService;
import com.sleekydz86.finsight.core.ulink.domain.port.in.dto.UlinkItemResponse;
import com.sleekydz86.finsight.core.ulink.service.UlinkItemService;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.admin.AdminStatsOverviewResponse;
import com.sleekydz86.finsight.core.user.domain.port.out.dto.UserResponse;
import com.sleekydz86.finsight.core.user.service.AdminStatsService;
import com.sleekydz86.finsight.core.user.service.UserApplicationService;
import com.sleekydz86.finsight.web.mcp.McpCaller;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "spring.ai.mcp.server", name = "enabled", havingValue = "true")
public class AdminMcpTools {

    private final UserApplicationService userApplicationService;
    private final HealthQueryUseCase healthQueryUseCase;
    private final YoutubeMediaAdminUseCase youtubeMediaAdminUseCase;
    private final MainimgItemService mainimgItemService;
    private final PopupItemService popupItemService;
    private final UlinkItemService ulinkItemService;
    private final NotificationQueryUseCase notificationQueryUseCase;
    private final EmailLogService emailLogService;
    private final AdminStatsService adminStatsService;
    private final BoardQueryService boardQueryService;

    public AdminMcpTools(
            UserApplicationService userApplicationService,
            HealthQueryUseCase healthQueryUseCase,
            YoutubeMediaAdminUseCase youtubeMediaAdminUseCase,
            MainimgItemService mainimgItemService,
            PopupItemService popupItemService,
            UlinkItemService ulinkItemService,
            NotificationQueryUseCase notificationQueryUseCase,
            EmailLogService emailLogService,
            AdminStatsService adminStatsService,
            BoardQueryService boardQueryService) {
        this.userApplicationService = userApplicationService;
        this.healthQueryUseCase = healthQueryUseCase;
        this.youtubeMediaAdminUseCase = youtubeMediaAdminUseCase;
        this.mainimgItemService = mainimgItemService;
        this.popupItemService = popupItemService;
        this.ulinkItemService = ulinkItemService;
        this.notificationQueryUseCase = notificationQueryUseCase;
        this.emailLogService = emailLogService;
        this.adminStatsService = adminStatsService;
        this.boardQueryService = boardQueryService;
    }

    @Tool(description = "관리자 전용. 사용자 목록을 조회합니다. 읽기 전용입니다.")
    public List<AdminUserItem> listAdminUsers(
            @ToolParam(description = "페이지 번호 (0부터)", required = false) Integer page,
            @ToolParam(description = "페이지 크기 (1~20)", required = false) Integer size) {
        McpCaller.requireAdmin();
        return userApplicationService.getUsers(PageRequest.of(pageOf(page), clamp(size, 20)))
                .getContent()
                .stream()
                .map(AdminMcpTools::user)
                .toList();
    }

    @Tool(description = "관리자 전용. 서버 상태를 조회합니다. 읽기 전용입니다.")
    public HealthItem getServerHealth() {
        McpCaller.requireAdmin();
        HealthStatus status = healthQueryUseCase.getOverallHealth();
        if (status == null) {
            return new HealthItem(null, null);
        }
        return new HealthItem(status.getStatus(), status.getMessage());
    }

    @Tool(description = "관리자 전용. 영상 관리 목록을 조회합니다. 읽기 전용입니다.")
    public List<AdminVideoItem> listAdminVideos(
            @ToolParam(description = "페이지 번호 (0부터)", required = false) Integer page,
            @ToolParam(description = "페이지 크기 (1~20)", required = false) Integer size) {
        McpCaller.requireAdmin();
        YoutubeAdminVideoSearchRequest request = new YoutubeAdminVideoSearchRequest();
        request.setPage(pageOf(page));
        request.setSize(clamp(size, 20));
        PaginationResponse<YoutubeVideoListResponse> result = youtubeMediaAdminUseCase.getAdminVideos(request);
        if (result == null || result.getContent() == null) {
            return List.of();
        }
        return result.getContent().stream().map(AdminMcpTools::video).toList();
    }

    @Tool(description = "관리자 전용. 메인이미지 목록을 조회합니다. 읽기 전용입니다.")
    public List<NamedItem> listMainImages() {
        McpCaller.requireAdmin();
        return content(mainimgItemService.list(null, false, 0, 20)).stream()
                .map(item -> new NamedItem(item.getId(), item.getImageName(), item.getReflectYn()))
                .toList();
    }

    @Tool(description = "관리자 전용. 팝업 목록을 조회합니다. 읽기 전용입니다.")
    public List<NamedItem> listPopups() {
        McpCaller.requireAdmin();
        return content(popupItemService.list(null, false, 0, 20)).stream()
                .map(item -> new NamedItem(item.getId(), item.getTitle(), item.getNoticeActive()))
                .toList();
    }

    @Tool(description = "관리자 전용. 통합링크 목록을 조회합니다. 읽기 전용입니다.")
    public List<LinkItem> listUlinks() {
        McpCaller.requireAdmin();
        return content(ulinkItemService.list(null, null, false, 0, 20)).stream()
                .map(item -> new LinkItem(item.getId(), item.getLinkName(), item.getLinkUrl(), item.getOpenYn()))
                .toList();
    }

    @Tool(description = "관리자 전용. 최근 7일 알림 발송 목록을 조회합니다. 읽기 전용입니다.")
    public List<NoticeItem> listAdminNotifications() {
        McpCaller.requireAdmin();
        LocalDateTime end = LocalDateTime.now();
        List<Notification> found = notificationQueryUseCase.getNotificationsByDateRange(end.minusDays(7), end);
        if (found == null) {
            return List.of();
        }
        return found.stream().limit(20).map(AdminMcpTools::notice).toList();
    }

    @Tool(description = "관리자 전용. 메일 발송 이력을 조회합니다. 본문과 IP는 포함하지 않습니다. 읽기 전용입니다.")
    public List<MailItem> listEmailLogs(
            @ToolParam(description = "페이지 번호 (0부터)", required = false) Integer page) {
        McpCaller.requireAdmin();
        return emailLogService.search(new EmailLogSearchCriteria(null, null, null, null, null, null, null),
                        PageRequest.of(pageOf(page), 20))
                .getContent()
                .stream()
                .map(AdminMcpTools::mail)
                .toList();
    }

    @Tool(description = "관리자 전용. 사용자·게시글·댓글·뉴스 건수 통계를 조회합니다. 읽기 전용입니다.")
    public StatsItem getAdminStats() {
        McpCaller.requireAdmin();
        AdminStatsOverviewResponse overview = adminStatsService.overview();
        return new StatsItem(
                overview.totalUsers(),
                overview.approvedUsers(),
                overview.pendingUsers(),
                overview.totalBoards(),
                overview.totalComments(),
                overview.totalNews());
    }

    @Tool(description = "관리자 전용. 신고된 게시글 목록을 조회합니다. 읽기 전용입니다.")
    public List<ReportItem> listReportedBoards() {
        McpCaller.requireAdmin();
        List<BoardListResponse> boards = boardQueryService.getReportedBoards();
        if (boards == null) {
            return List.of();
        }
        return boards.stream().limit(30).map(AdminMcpTools::report).toList();
    }

    private static <T> List<T> content(PaginationResponse<T> page) {
        if (page == null || page.getContent() == null) {
            return List.of();
        }
        return page.getContent();
    }

    private static AdminUserItem user(UserResponse user) {
        return new AdminUserItem(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getStatus() == null ? null : user.getStatus().name(),
                user.getRole() == null ? null : user.getRole().name());
    }

    private static AdminVideoItem video(YoutubeVideoListResponse video) {
        return new AdminVideoItem(
                video.getBoardId(),
                video.getTitle(),
                video.getVideoId(),
                video.getCategory(),
                video.getImportStatus() == null ? null : video.getImportStatus().name());
    }

    private static NoticeItem notice(Notification notification) {
        return new NoticeItem(
                notification.getId(),
                notification.getTitle(),
                notification.getType() == null ? null : notification.getType().name(),
                notification.getStatus() == null ? null : notification.getStatus().name(),
                notification.getSentAt() == null ? null : notification.getSentAt().toString());
    }

    private static MailItem mail(EmailLogResponse log) {
        return new MailItem(
                log.id(),
                log.subject(),
                log.recipient(),
                log.status() == null ? null : log.status().name(),
                log.sentAt() == null ? null : log.sentAt().toString());
    }

    private static ReportItem report(BoardListResponse board) {
        return new ReportItem(board.getId(), board.getTitle(), board.getAuthorEmail(), board.getStatus() == null ? null : board.getStatus().name());
    }

    private static int pageOf(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }

    private static int clamp(Integer size, int max) {
        int value = size == null ? 20 : size;
        return Math.min(max, Math.max(1, value));
    }

    public record AdminUserItem(Long id, String username, String nickname, String email, String status, String role) {
    }

    public record HealthItem(String status, String message) {
    }

    public record AdminVideoItem(Long boardId, String title, String videoId, String category, String importStatus) {
    }

    public record NamedItem(String id, String name, String state) {
    }

    public record LinkItem(String id, String name, String url, String openYn) {
    }

    public record NoticeItem(Long id, String title, String type, String status, String sentAt) {
    }

    public record MailItem(Long id, String subject, String recipient, String status, String sentAt) {
    }

    public record StatsItem(long totalUsers, long approvedUsers, long pendingUsers, long totalBoards, long totalComments, long totalNews) {
    }

    public record ReportItem(Long id, String title, String authorEmail, String status) {
    }
}
