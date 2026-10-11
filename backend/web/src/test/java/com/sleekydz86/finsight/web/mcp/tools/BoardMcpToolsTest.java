package com.sleekydz86.finsight.web.mcp.tools;

import com.sleekydz86.finsight.core.board.domain.BoardStatus;
import com.sleekydz86.finsight.core.board.domain.BoardType;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardDetailResponse;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardListResponse;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardSearchRequest;
import com.sleekydz86.finsight.core.board.service.BoardQueryService;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoardMcpToolsTest {

    private FixedBoardQuery boardQuery;
    private BoardMcpTools tools;

    @BeforeEach
    void setUp() {
        boardQuery = new FixedBoardQuery();
        tools = new BoardMcpTools(boardQuery);
    }

    @Test
    void listBoards_keepsCountsAndPaging() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 1, 9, 0);
        boardQuery.result = new PaginationResponse<>(List.of(notice(createdAt, 4, 2)), 0, 10, 8);

        BoardMcpTools.BoardListResult result = tools.listBoards("notice", " 점검 ", 2, 50);

        assertThat(boardQuery.last.getBoardType()).isEqualTo(BoardType.NOTICE);
        assertThat(boardQuery.last.getKeyword()).isEqualTo("점검");
        assertThat(boardQuery.last.getPage()).isEqualTo(2);
        assertThat(boardQuery.last.getSize()).isEqualTo(30);
        assertThat(result.count()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(8);
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(30);
        assertThat(result.items()).containsExactly(new BoardMcpTools.BoardSummary(
                3L, "점검 안내", "NOTICE", 4, 2, createdAt.toString(), "ops@example.com"));
    }

    @Test
    void getBoardDetail_doesNotAskToIncrementViews() {
        boardQuery.detail = new BoardDetailResponse(
                3L,
                "점검 안내",
                "본문",
                "",
                "미리보기",
                "ops@example.com",
                BoardType.NOTICE,
                BoardStatus.ACTIVE,
                4,
                0,
                0,
                2,
                0,
                List.of(),
                List.of(),
                LocalDateTime.of(2026, 10, 1, 9, 0),
                LocalDateTime.of(2026, 10, 1, 9, 0),
                null,
                false);

        BoardMcpTools.BoardDetailResult result = tools.getBoardDetail(3L);

        assertThat(boardQuery.incrementViewCount).isFalse();
        assertThat(result.contentPreview()).isEqualTo("미리보기");
        assertThat(result.viewCount()).isEqualTo(4);
    }

    @Test
    void listPopularBoards_usesPopularQuery() {
        boardQuery.popular = List.of(notice(LocalDateTime.of(2026, 10, 1, 9, 0), 9, 1));

        BoardMcpTools.BoardListResult result = tools.listPopularBoards(1);

        assertThat(boardQuery.popularLimit).isEqualTo(1);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().viewCount()).isEqualTo(9);
    }

    @Test
    void listBoards_blankTypeMeansAllBoards() {
        tools.listBoards("  ", null, null, null);

        assertThat(boardQuery.last.getBoardType()).isNull();
        assertThat(boardQuery.last.getKeyword()).isNull();
        assertThat(boardQuery.last.getPage()).isZero();
        assertThat(boardQuery.last.getSize()).isEqualTo(10);
    }

    @Test
    void listBoards_rejectsUnknownType() {
        assertThatThrownBy(() -> tools.listBoards("SPY", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는 boardType 입니다: SPY");
    }

    private static BoardListResponse notice(LocalDateTime createdAt, int viewCount, int commentCount) {
        return new BoardListResponse(
                3L,
                "점검 안내",
                "ops@example.com",
                BoardType.NOTICE,
                BoardStatus.ACTIVE,
                viewCount,
                0,
                0,
                commentCount,
                List.of(),
                "",
                createdAt,
                createdAt,
                false);
    }

    private static final class FixedBoardQuery extends BoardQueryService {
        private BoardSearchRequest last;
        private PaginationResponse<BoardListResponse> result = new PaginationResponse<>(List.of(), 0, 10, 0);
        private BoardDetailResponse detail;
        private boolean incrementViewCount = true;
        private List<BoardListResponse> popular = List.of();
        private int popularLimit;

        private FixedBoardQuery() {
            super(null, null, null, null, null);
        }

        @Override
        public PaginationResponse<BoardListResponse> getBoards(BoardSearchRequest request) {
            last = request;
            return result;
        }

        @Override
        public BoardDetailResponse getBoardDetail(
                Long boardId, String viewerEmail, boolean staffViewer, boolean incrementViewCount) {
            this.incrementViewCount = incrementViewCount;
            return detail;
        }

        @Override
        public List<BoardListResponse> getPopularBoards(int limit) {
            popularLimit = limit;
            return popular;
        }
    }
}
