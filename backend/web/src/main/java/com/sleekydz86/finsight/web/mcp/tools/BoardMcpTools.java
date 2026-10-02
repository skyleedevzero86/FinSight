package com.sleekydz86.finsight.web.mcp.tools;

import com.sleekydz86.finsight.core.board.domain.BoardType;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardListResponse;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardSearchRequest;
import com.sleekydz86.finsight.core.board.service.BoardQueryService;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "spring.ai.mcp.server", name = "enabled", havingValue = "true")
public class BoardMcpTools {

    private final BoardQueryService boardQueryService;

    public BoardMcpTools(BoardQueryService boardQueryService) {
        this.boardQueryService = boardQueryService;
    }

    @Tool(description = "게시판 글을 검색합니다. boardType 예: FREE, NOTICE, QNA, COMMUNITY, MEDIA. 읽기 전용입니다.")
    public BoardListResult listBoards(
            @ToolParam(description = "게시판 타입 (FREE/NOTICE/QNA/COMMUNITY/MEDIA)", required = false) String boardType,
            @ToolParam(description = "검색 키워드", required = false) String keyword,
            @ToolParam(description = "페이지 번호 (0부터)", required = false) Integer page,
            @ToolParam(description = "페이지 크기 (1~30)", required = false) Integer size) {
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = clamp(size, 1, 30, 10);
        BoardSearchRequest request = BoardSearchRequest.builder()
                .boardType(parseBoardType(boardType))
                .keyword(blankToNull(keyword))
                .page(safePage)
                .size(safeSize)
                .build();
        PaginationResponse<BoardListResponse> result = boardQueryService.getBoards(request);
        List<BoardSummary> items = result.getContent().stream()
                .map(b -> new BoardSummary(
                        b.getId(),
                        b.getTitle(),
                        b.getBoardType() != null ? b.getBoardType().name() : null,
                        b.getViewCount(),
                        b.getCommentCount(),
                        b.getCreatedAt() != null ? b.getCreatedAt().toString() : null))
                .toList();
        return new BoardListResult(items.size(), result.getTotalElements(), safePage, safeSize, items);
    }

    private static BoardType parseBoardType(String boardType) {
        if (boardType == null || boardType.isBlank()) {
            return null;
        }
        try {
            return BoardType.valueOf(boardType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "지원하지 않는 boardType 입니다: " + boardType + " (FREE, NOTICE, QNA, COMMUNITY, MEDIA)");
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static int clamp(Integer value, int min, int max, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return Math.min(Math.max(value, min), max);
    }

    public record BoardSummary(
            Long id,
            String title,
            String boardType,
            int viewCount,
            int commentCount,
            String createdAt) {
    }

    public record BoardListResult(
            int count,
            long totalElements,
            int page,
            int size,
            List<BoardSummary> items) {
    }
}
