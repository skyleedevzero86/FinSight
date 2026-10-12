package com.sleekydz86.finsight.web.mcp.tools;

import com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardListResponse;
import com.sleekydz86.finsight.core.board.service.BoardQueryService;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.MyFavoriteItemResponse;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.MyFavoritePageResponse;
import com.sleekydz86.finsight.core.media.livevod.service.LiveVodEngagementService;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.user.service.UserService;
import com.sleekydz86.finsight.web.mcp.McpCaller;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "spring.ai.mcp.server", name = "enabled", havingValue = "true")
public class AccountMcpTools {

    private final LiveVodEngagementService liveVodEngagementService;
    private final BoardQueryService boardQueryService;
    private final UserService userService;

    public AccountMcpTools(
            LiveVodEngagementService liveVodEngagementService,
            BoardQueryService boardQueryService,
            UserService userService) {
        this.liveVodEngagementService = liveVodEngagementService;
        this.boardQueryService = boardQueryService;
        this.userService = userService;
    }

    @Tool(description = "로그인한 사용자의 LIVE/VOD 즐겨찾기를 조회합니다. 읽기 전용입니다.")
    public FavoriteListResult listMyFavorites(
            @ToolParam(description = "페이지 번호 (0부터)", required = false) Integer page,
            @ToolParam(description = "페이지 크기 (1~30)", required = false) Integer size) {
        String email = McpCaller.requireEmail();
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = clamp(size);
        MyFavoritePageResponse result = liveVodEngagementService.listMyFavorites(email, safePage, safeSize);
        List<FavoriteItem> items = result.items().stream().map(AccountMcpTools::favorite).toList();
        return new FavoriteListResult(items.size(), result.totalElements(), result.page(), result.size(), items);
    }

    @Tool(description = "로그인한 사용자의 게시글을 조회합니다. 읽기 전용입니다.")
    public MyBoardListResult listMyPosts(
            @ToolParam(description = "페이지 번호 (0부터)", required = false) Integer page,
            @ToolParam(description = "페이지 크기 (1~30)", required = false) Integer size) {
        String email = McpCaller.requireEmail();
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = clamp(size);
        PaginationResponse<BoardListResponse> result = boardQueryService.getMyBoards(email, safePage, safeSize);
        List<MyBoardItem> items = result == null || result.getContent() == null
                ? List.of()
                : result.getContent().stream().map(AccountMcpTools::post).toList();
        long total = result == null ? 0 : result.getTotalElements();
        return new MyBoardListResult(items.size(), total, safePage, safeSize, items);
    }

    @Tool(description = "로그인한 사용자의 관심 종목을 조회합니다. 읽기 전용입니다. 포트폴리오 화면은 아직 서버 데이터가 없습니다.")
    public List<String> listMyWatchlist() {
        String email = McpCaller.requireEmail();
        Long userId = userService.findByEmail(email)
                .map(user -> user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "로그인 사용자를 찾을 수 없습니다. 입력값: " + email));
        List<TargetCategory> watchlist = userService.getUserWatchlist(userId);
        if (watchlist == null) {
            return List.of();
        }
        return watchlist.stream().map(TargetCategory::name).toList();
    }

    private static FavoriteItem favorite(MyFavoriteItemResponse item) {
        return new FavoriteItem(item.videoId(), item.title(), item.channelTitle(), item.savedAt());
    }

    private static MyBoardItem post(BoardListResponse board) {
        return new MyBoardItem(
                board.getId(),
                board.getTitle(),
                board.getBoardType() == null ? null : board.getBoardType().name(),
                board.getCreatedAt() == null ? null : board.getCreatedAt().toString());
    }

    private static int clamp(Integer size) {
        int value = size == null ? 10 : size;
        return Math.min(30, Math.max(1, value));
    }

    public record FavoriteItem(String videoId, String title, String channelTitle, String savedAt) {
    }

    public record FavoriteListResult(int count, long totalElements, int page, int size, List<FavoriteItem> items) {
    }

    public record MyBoardItem(Long id, String title, String boardType, String createdAt) {
    }

    public record MyBoardListResult(int count, long totalElements, int page, int size, List<MyBoardItem> items) {
    }
}
