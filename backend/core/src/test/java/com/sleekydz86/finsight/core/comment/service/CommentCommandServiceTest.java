package com.sleekydz86.finsight.core.comment.service;

import com.sleekydz86.finsight.core.comment.domain.Comment;
import com.sleekydz86.finsight.core.comment.domain.CommentStatus;
import com.sleekydz86.finsight.core.comment.domain.CommentType;
import com.sleekydz86.finsight.core.comment.domain.port.in.dto.CommentCreateRequest;
import com.sleekydz86.finsight.core.comment.domain.port.out.CommentPersistencePort;
import com.sleekydz86.finsight.core.comment.domain.port.out.CommentReactionPersistencePort;
import com.sleekydz86.finsight.core.comment.domain.port.out.CommentReportPersistencePort;
import com.sleekydz86.finsight.core.global.exception.CommentNotFoundException;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentCommandServiceTest {

    @Mock
    private CommentPersistencePort commentPersistencePort;

    @Mock
    private CommentReactionPersistencePort commentReactionPersistencePort;

    @Mock
    private CommentReportPersistencePort commentReportPersistencePort;

    @Mock
    private com.sleekydz86.finsight.core.board.domain.port.out.BoardPersistencePort boardPersistencePort;

    @Mock
    private NewsPersistencePort newsPersistencePort;

    @InjectMocks
    private CommentCommandService commentCommandService;

    private CommentCreateRequest commentCreateRequest;
    private Comment comment;

    @BeforeEach
    void setUp() {
        commentCreateRequest = new CommentCreateRequest(
                "테스트 댓글 내용",
                CommentType.NEWS,
                1L,
                null
        );

        comment = Comment.builder()
                .id(1L)
                .content("테스트 댓글 내용")
                .authorEmail("test@example.com")
                .commentType(CommentType.NEWS)
                .targetId(1L)
                .parentId(null)
                .status(CommentStatus.ACTIVE)
                .likeCount(0)
                .dislikeCount(0)
                .reportCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("댓글 생성 성공")
    void createsComment() {
        // given
        when(newsPersistencePort.findById(1L)).thenReturn(Optional.of(new News()));
        when(commentPersistencePort.save(any(Comment.class))).thenReturn(comment);

        // when
        Comment result = commentCommandService.createComment("test@example.com", "USER", commentCreateRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("테스트 댓글 내용");
        assertThat(result.getAuthorEmail()).isEqualTo("test@example.com");
        assertThat(result.getCommentType()).isEqualTo(CommentType.NEWS);
        assertThat(result.getTargetId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 댓글 수정시 예외 발생")
    void throwsWhenUpdatingMissingComment() {
        // given
        when(commentPersistencePort.findById(1L)).thenReturn(Optional.empty());

        // when

        // then
        assertThatThrownBy(() -> commentCommandService.updateComment("test@example.com", 1L,
                new com.sleekydz86.finsight.core.comment.domain.port.in.dto.CommentUpdateRequest("수정된 내용")))
                .isInstanceOf(com.sleekydz86.finsight.core.global.exception.NewsNotFoundException.class);
    }

    @Test
    @DisplayName("권한이 없는 사용자가 댓글 수정시 예외 발생")
    void throwsWhenUserWithoutPermissionUpdatesComment() {
        // given
        when(commentPersistencePort.findById(1L)).thenReturn(Optional.of(comment));

        // when

        // then
        assertThatThrownBy(() -> commentCommandService.updateComment("other@example.com", 1L,
                new com.sleekydz86.finsight.core.comment.domain.port.in.dto.CommentUpdateRequest("수정된 내용")))
                .isInstanceOf(com.sleekydz86.finsight.core.global.exception.InsufficientPermissionException.class);
    }
}
