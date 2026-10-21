package com.sleekydz86.finsight.core.portfolio.service;

import com.sleekydz86.finsight.core.global.exception.InsufficientPermissionException;
import com.sleekydz86.finsight.core.global.exception.ValidationException;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioAssetJpaRepository;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareJpaEntity;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareJpaRepository;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioSnapshotJpaRepository;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioShareFeed.PortfolioShareCard;
import com.sleekydz86.finsight.core.search.config.ElasticsearchProperties;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioShareModerationTest {

    @Mock
    private PortfolioAssetJpaRepository assetRepository;

    @Mock
    private PortfolioSnapshotJpaRepository snapshotRepository;

    @Mock
    private PortfolioShareJpaRepository shareRepository;

    @Mock
    private UserPersistencePort userPersistencePort;

    @Mock
    private PortfolioShareFeedbackService feedbackService;

    private PortfolioService portfolioService;

    @BeforeEach
    void setUp() {
        portfolioService = new PortfolioService(
                assetRepository,
                snapshotRepository,
                shareRepository,
                new PortfolioGoalSearch(new ElasticsearchProperties(), null),
                userPersistencePort,
                org.mockito.Mockito.mock(PortfolioGoalAlarm.class),
                feedbackService);
    }

    @Test
    @DisplayName("관리자는 공유 카드를 블라인드한다")
    void adminBlindsShareCard() {
        // given
        PortfolioShareJpaEntity share = share();
        when(shareRepository.findById(4L)).thenReturn(Optional.of(share));
        when(feedbackService.toCard(any())).thenAnswer(invocation ->
                PortfolioShareCards.from(invocation.getArgument(0), 0, 0, 0));

        // when
        PortfolioShareCard card = portfolioService.moderate("MANAGER", 4L, "BLIND");

        // then
        assertThat(share.getModerationStatus()).isEqualTo("BLIND");
        assertThat(card.moderationStatus()).isEqualTo("BLIND");
    }

    @Test
    @DisplayName("삭제된 공유는 다시 수정하지 않는다")
    void doesNotEditDeletedShareAgain() {
        // given
        PortfolioShareJpaEntity share = share();

        // when
        share.applyModeration("REMOVED");

        // given
        when(shareRepository.findById(4L)).thenReturn(Optional.of(share));

        // then
        assertThatThrownBy(() -> portfolioService.moderate("ADMIN", 4L, "WARN"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("삭제된 게시물은 수정할 수 없습니다");
        assertThat(share.getModerationStatus()).isEqualTo("REMOVED");
    }

    @Test
    @DisplayName("일반 사용자는 공유를 조치할 수 없다")
    void regularUserCannotModerateShare() {
        // given

        // when

        // then
        assertThatThrownBy(() -> portfolioService.moderate("USER", 4L, "REMOVED"))
                .isInstanceOf(InsufficientPermissionException.class);
        verify(shareRepository, never()).findById(4L);
    }

    private static PortfolioShareJpaEntity share() {
        return new PortfolioShareJpaEntity(
                0L,
                "studio_kim",
                "디자인 스튜디오를 운영하면서 2천만원을 모으는 게 제 목표입니다.",
                "PUBLIC",
                "1억 만들기",
                "2,000만원",
                60,
                "+3.7%",
                "60%",
                true,
                true,
                LocalDateTime.of(2026, 10, 9, 9, 10));
    }
}
