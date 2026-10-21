package com.sleekydz86.finsight.core.board.service;

import com.sleekydz86.finsight.core.board.domain.Board;
import com.sleekydz86.finsight.core.board.domain.BoardStatus;
import com.sleekydz86.finsight.core.board.domain.BoardType;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardDetailResponse;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardPersistencePort;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardReactionPersistencePort;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardScrapPersistencePort;
import com.sleekydz86.finsight.core.board.markdown.MarkdownRenderingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardQueryServiceViewCountTest {

    @Mock
    private BoardPersistencePort boardPersistencePort;

    @Mock
    private BoardReactionPersistencePort boardReactionPersistencePort;

    @Mock
    private BoardScrapPersistencePort boardScrapPersistencePort;

    @Mock
    private BoardViewCountService boardViewCountService;

    @Test
    @DisplayName("상세 응답의 조회수는 증가 전에 읽은 값이다")
    void detailViewCountIsTheValueReadBeforeIncrement() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 10, 9, 1, 0);
        Board loaded = new Board(
                1L,
                "공지",
                "본문",
                "author@example.com",
                BoardType.NOTICE,
                BoardStatus.ACTIVE,
                7,
                0,
                0,
                0,
                0,
                List.of(),
                List.of(),
                now,
                now,
                List.of(),
                false);
        when(boardPersistencePort.findById(1L)).thenReturn(Optional.of(loaded));
        BoardQueryService service = new BoardQueryService(
                boardPersistencePort,
                boardReactionPersistencePort,
                boardScrapPersistencePort,
                new MarkdownRenderingService(),
                boardViewCountService);

        // when
        BoardDetailResponse response = service.getBoardDetail(1L, null, false, true);

        // then
        verify(boardViewCountService).incrementSafely(1L);
        assertThat(response.getViewCount()).isEqualTo(7);
    }
}
