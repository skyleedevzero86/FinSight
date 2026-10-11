package com.sleekydz86.finsight.core.portfolio.service;

import com.sleekydz86.finsight.core.global.exception.ValidationException;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareCommentJpaEntity;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareCommentJpaRepository;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareJpaEntity;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareJpaRepository;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioShareCommentItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioShareCommentServiceTest {

    @Mock
    private PortfolioShareJpaRepository shareRepository;

    @Mock
    private PortfolioShareCommentJpaRepository commentRepository;

    private PortfolioShareCommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new PortfolioShareCommentService(shareRepository, commentRepository);
    }

    @Test
    void 댓글을_저장하고_작성자_이름을_가린다() {
        PortfolioShareJpaEntity share = share();
        PortfolioShareCommentJpaEntity stored = new PortfolioShareCommentJpaEntity(
                4L, 7L, "studio_kim", "응원합니다", LocalDateTime.of(2026, 10, 10, 19, 40));
        ReflectionTestUtils.setField(stored, "id", 9L);
        when(shareRepository.findById(4L)).thenReturn(Optional.of(share));
        when(commentRepository.findTop30ByShareIdOrderByIdDesc(4L)).thenReturn(List.of(stored));

        List<PortfolioShareCommentItem> comments = commentService.add(7L, "studio_kim", 4L, " 응원합니다 ");

        ArgumentCaptor<PortfolioShareCommentJpaEntity> captor = ArgumentCaptor.forClass(PortfolioShareCommentJpaEntity.class);
        verify(commentRepository).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("응원합니다");
        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).authorName()).isEqualTo("s*********");
        assertThat(comments.get(0).content()).isEqualTo("응원합니다");
    }

    @Test
    void 삭제된_게시물에는_댓글을_남기지_않는다() {
        PortfolioShareJpaEntity share = share();
        share.applyModeration("REMOVED");
        when(shareRepository.findById(4L)).thenReturn(Optional.of(share));

        assertThatThrownBy(() -> commentService.add(7L, "studio_kim", 4L, "댓글"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("삭제된 게시물");
    }

    private static PortfolioShareJpaEntity share() {
        PortfolioShareJpaEntity entity = new PortfolioShareJpaEntity(
                0L,
                "month_park",
                "이번 달 순자산 변화",
                "PUBLIC",
                "1억 만들기",
                "약 2,000만원",
                20,
                null,
                "20%",
                true,
                false,
                LocalDateTime.of(2026, 10, 9, 8, 40));
        ReflectionTestUtils.setField(entity, "id", 4L);
        return entity;
    }
}
