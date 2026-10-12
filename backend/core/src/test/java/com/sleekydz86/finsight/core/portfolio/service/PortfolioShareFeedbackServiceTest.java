package com.sleekydz86.finsight.core.portfolio.service;

import com.sleekydz86.finsight.core.global.exception.ValidationException;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareJpaEntity;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareJpaRepository;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareReactionJpaEntity;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareReactionJpaRepository;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareReportJpaRepository;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioShareDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioShareFeedbackServiceTest {

    @Mock
    private PortfolioShareJpaRepository shareRepository;

    @Mock
    private PortfolioShareReactionJpaRepository reactionRepository;

    @Mock
    private PortfolioShareReportJpaRepository reportRepository;

    private PortfolioShareFeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        feedbackService = new PortfolioShareFeedbackService(shareRepository, reactionRepository, reportRepository);
    }

    @Test
    void 싫어요는_경고로_보이고_같은_반응을_다시_누르면_지운다() {
        PortfolioShareJpaEntity share = share();
        when(shareRepository.findById(4L)).thenReturn(Optional.of(share));
        when(reactionRepository.findByShareIdAndUserId(4L, 7L)).thenReturn(Optional.empty());
        when(reactionRepository.countByShareIdAndReactionType(4L, "LIKE")).thenReturn(0L);
        when(reactionRepository.countByShareIdAndReactionType(4L, "DISLIKE")).thenReturn(1L);
        when(reportRepository.countByShareId(4L)).thenReturn(0L);
        when(reportRepository.findByShareIdAndUserId(4L, 7L)).thenReturn(Optional.empty());

        PortfolioShareDetail detail = feedbackService.react(7L, 4L, "DISLIKE");

        ArgumentCaptor<PortfolioShareReactionJpaEntity> captor = ArgumentCaptor.forClass(PortfolioShareReactionJpaEntity.class);
        verify(reactionRepository).save(captor.capture());
        assertThat(captor.getValue().getReactionType()).isEqualTo("DISLIKE");
        assertThat(detail.card().moderationStatus()).isEqualTo("WARN");
        assertThat(detail.card().dislikeCount()).isEqualTo(1);
    }

    @Test
    void 신고하면_블라인드로_보인다() {
        PortfolioShareJpaEntity share = share();
        when(shareRepository.findById(4L)).thenReturn(Optional.of(share));
        when(reportRepository.findByShareIdAndUserId(4L, 7L)).thenReturn(Optional.empty());
        when(reactionRepository.countByShareIdAndReactionType(4L, "LIKE")).thenReturn(3L);
        when(reactionRepository.countByShareIdAndReactionType(4L, "DISLIKE")).thenReturn(0L);
        when(reportRepository.countByShareId(4L)).thenReturn(1L);
        when(reactionRepository.findByShareIdAndUserId(4L, 7L)).thenReturn(Optional.empty());

        PortfolioShareDetail detail = feedbackService.report(7L, 4L, "투자 사기", "계좌번호를 요구합니다");

        ArgumentCaptor<com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareReportJpaEntity> captor =
                ArgumentCaptor.forClass(com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareReportJpaEntity.class);
        verify(reportRepository).save(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo("투자 사기");
        assertThat(captor.getValue().getNote()).isEqualTo("계좌번호를 요구합니다");
        assertThat(detail.card().moderationStatus()).isEqualTo("BLIND");
        assertThat(detail.card().reportCount()).isEqualTo(1);
        assertThat(detail.card().message()).contains("부채를 줄이는");
    }

    @Test
    void 블라인드_카드에는_반응할_수_없다() {
        PortfolioShareJpaEntity share = share();
        share.applyModeration("BLIND");
        when(shareRepository.findById(4L)).thenReturn(Optional.of(share));
        when(reactionRepository.countByShareIdAndReactionType(4L, "DISLIKE")).thenReturn(0L);
        when(reportRepository.countByShareId(4L)).thenReturn(0L);

        assertThatThrownBy(() -> feedbackService.react(7L, 4L, "LIKE"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("블라인드");
        verify(reactionRepository, never()).save(any());
    }

    private static PortfolioShareJpaEntity share() {
        PortfolioShareJpaEntity entity = new PortfolioShareJpaEntity(
                0L,
                "month_park",
                "디자이너 생활을 하면서 2천만원, 투자수익보다 부채를 줄이는 게 제 목표입니다.",
                "PUBLIC",
                "1억 만들기",
                "2,000만원",
                60,
                "+3.7%",
                "60%",
                true,
                true,
                LocalDateTime.of(2026, 10, 9, 8, 40));
        ReflectionTestUtils.setField(entity, "id", 4L);
        return entity;
    }
}
