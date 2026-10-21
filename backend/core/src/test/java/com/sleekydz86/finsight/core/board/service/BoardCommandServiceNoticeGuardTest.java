package com.sleekydz86.finsight.core.board.service;

import com.sleekydz86.finsight.core.board.domain.Board;
import com.sleekydz86.finsight.core.board.domain.BoardType;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardCreateRequest;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardUpdateRequest;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardFilePersistencePort;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardPersistencePort;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardReactionPersistencePort;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardReportPersistencePort;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardScrapPersistencePort;
import com.sleekydz86.finsight.core.board.domain.BoardStatus;
import com.sleekydz86.finsight.core.global.exception.InsufficientPermissionException;
import com.sleekydz86.finsight.core.global.exception.ValidationException;
import com.sleekydz86.finsight.core.inbox.service.InboxService;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.UserRole;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardCommandServiceNoticeGuardTest {

    @Mock
    private BoardPersistencePort boardPersistencePort;
    @Mock
    private BoardReactionPersistencePort boardReactionPersistencePort;
    @Mock
    private BoardReportPersistencePort boardReportPersistencePort;
    @Mock
    private BoardScrapPersistencePort boardScrapPersistencePort;
    @Mock
    private BoardFilePersistencePort boardFilePersistencePort;
    @Mock
    private UserPersistencePort userPersistencePort;
    @Mock
    private InboxService inboxService;

    private BoardCommandService service;

    @BeforeEach
    void setUp() {
        service = new BoardCommandService(
                boardPersistencePort,
                boardReactionPersistencePort,
                boardReportPersistencePort,
                boardScrapPersistencePort,
                boardFilePersistencePort,
                userPersistencePort,
                inboxService);
    }

    @Test
    @DisplayName("일반 사용자가 공지를 만들면 거절한다")
    void createNotice_whenUser_rejects() {
        // given
        when(userPersistencePort.findByEmail("user@test.com"))
                .thenReturn(Optional.of(user("user@test.com", UserRole.USER)));

        // when

        // then
        assertThatThrownBy(() -> service.createBoard(
                "user@test.com",
                new BoardCreateRequest("제목", "내용", BoardType.NOTICE, List.of())))
                .isInstanceOf(InsufficientPermissionException.class)
                .hasMessageContaining("공지사항은 관리자만 작성할 수 있습니다");
        verify(boardPersistencePort, never()).save(any());
    }

    @Test
    @DisplayName("관리자가 공지를 만들면 저장한다")
    void createNotice_whenAdmin_saves() {
        // given
        when(userPersistencePort.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(user("admin@test.com", UserRole.ADMIN)));
        when(userPersistencePort.findAllActiveUsers()).thenReturn(List.of());
        when(boardPersistencePort.save(any(Board.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Board saved = service.createBoard(
                "admin@test.com",
                new BoardCreateRequest("점검 안내", "본문", BoardType.NOTICE, List.of()));

        // then
        assertThat(saved.getBoardType()).isEqualTo(BoardType.NOTICE);
        assertThat(saved.getTitle()).isEqualTo("점검 안내");
    }

    @Test
    @DisplayName("작성자가 운영자가 아니면 공지 수정을 거절한다")
    void updateNotice_whenAuthorIsNotStaff_rejects() {
        // given
        when(boardPersistencePort.findById(7L)).thenReturn(Optional.of(notice()));

        // when

        // then
        assertThatThrownBy(() -> service.updateBoard(
                "user@test.com",
                "USER",
                7L,
                new BoardUpdateRequest("수정", "본문", List.of())))
                .isInstanceOf(InsufficientPermissionException.class)
                .hasMessageContaining("공지사항은 관리자만 수정·삭제할 수 있습니다");
        verify(boardPersistencePort, never()).save(any());
    }

    @Test
    @DisplayName("매니저가 공지를 수정하면 저장한다")
    void updateNotice_whenManager_saves() {
        // given
        when(boardPersistencePort.findById(7L)).thenReturn(Optional.of(notice()));
        when(boardPersistencePort.save(any(Board.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Board updated = service.updateBoard(
                "manager@test.com",
                "ROLE_MANAGER",
                7L,
                new BoardUpdateRequest("수정된 제목", "본문", List.of()));

        // then
        assertThat(updated.getTitle()).isEqualTo("수정된 제목");
    }

    @Test
    @DisplayName("강조 공지가 네 번째면 거절한다")
    void createNotice_whenFourthHighlight_rejects() {
        // given
        when(userPersistencePort.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(user("admin@test.com", UserRole.ADMIN)));
        when(boardPersistencePort.countActiveHighlightedNotices(null)).thenReturn(3L);

        // when

        // then
        assertThatThrownBy(() -> service.createBoard(
                "admin@test.com",
                new BoardCreateRequest("넷째", "본문", BoardType.NOTICE, List.of(), BoardStatus.ACTIVE, true)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("강조 공지는 최대 3개까지");
        verify(boardPersistencePort, never()).save(any());
    }

    @Test
    @DisplayName("강조 자리가 남아 있으면 강조 공지를 저장한다")
    void createNotice_whenHighlightSlotRemains_savesHighlighted() {
        // given
        when(userPersistencePort.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(user("admin@test.com", UserRole.ADMIN)));
        when(userPersistencePort.findAllActiveUsers()).thenReturn(List.of());
        when(boardPersistencePort.countActiveHighlightedNotices(null)).thenReturn(2L);
        when(boardPersistencePort.save(any(Board.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Board saved = service.createBoard(
                "admin@test.com",
                new BoardCreateRequest("강조", "본문", BoardType.NOTICE, List.of(), BoardStatus.ACTIVE, true));

        // then
        assertThat(saved.isHighlighted()).isTrue();
    }

    @Test
    @DisplayName("공지 좋아요는 거절한다")
    void likeNotice_rejects() {
        // given
        when(boardPersistencePort.findById(7L)).thenReturn(Optional.of(notice()));

        // when

        // then
        assertThatThrownBy(() -> service.likeBoard("user@test.com", 7L))
                .isInstanceOf(InsufficientPermissionException.class)
                .hasMessageContaining("공지사항에는 좋아요와 싫어요를 남길 수 없습니다");
        verify(boardReactionPersistencePort, never()).findByBoardIdAndUserEmail(any(), any());
    }

    @Test
    @DisplayName("공지 싫어요는 거절한다")
    void dislikeNotice_rejects() {
        // given
        when(boardPersistencePort.findById(7L)).thenReturn(Optional.of(notice()));

        // when

        // then
        assertThatThrownBy(() -> service.dislikeBoard("reader@test.com", 7L))
                .isInstanceOf(InsufficientPermissionException.class)
                .hasMessageContaining("공지사항에는 좋아요와 싫어요를 남길 수 없습니다");
    }

    private static User user(String email, UserRole role) {
        return User.builder().email(email).role(role).build();
    }

    private static Board notice() {
        return Board.builder()
                .id(7L)
                .title("기존 공지")
                .content("기존 본문")
                .authorEmail("user@test.com")
                .boardType(BoardType.NOTICE)
                .build();
    }
}
