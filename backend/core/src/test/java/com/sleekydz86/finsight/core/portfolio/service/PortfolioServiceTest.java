package com.sleekydz86.finsight.core.portfolio.service;

import com.sleekydz86.finsight.core.global.exception.ValidationException;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioAssetJpaEntity;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioAssetJpaRepository;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareJpaEntity;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareJpaRepository;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioSnapshotJpaRepository;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioAssetCategory;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioAssetKind;
import com.sleekydz86.finsight.core.portfolio.domain.port.in.dto.PortfolioAssetCommand;
import com.sleekydz86.finsight.core.portfolio.domain.port.in.dto.PortfolioShareCommand;
import com.sleekydz86.finsight.core.search.config.ElasticsearchProperties;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PortfolioAssetJpaRepository assetRepository;

    @Mock
    private PortfolioSnapshotJpaRepository snapshotRepository;

    @Mock
    private PortfolioShareJpaRepository shareRepository;

    @Mock
    private UserPersistencePort userPersistencePort;

    private PortfolioService portfolioService;

    @BeforeEach
    void setUp() {
        portfolioService = new PortfolioService(
                assetRepository,
                snapshotRepository,
                shareRepository,
                new PortfolioGoalSearch(new ElasticsearchProperties(), null),
                userPersistencePort,
                mock(PortfolioGoalAlarm.class),
                mock(PortfolioShareFeedbackService.class));
    }

    @Test
    @DisplayName("이름이 비어 있으면 자산을 저장하지 않는다")
    void doesNotSaveAssetWhenNameIsBlank() {
        // given
        PortfolioAssetCommand command = new PortfolioAssetCommand("  ", "ASSET", "CASH", 10_000L, null, null, null, null, null, null);

        // then
        assertThatThrownBy(() -> portfolioService.register(1L, command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("자산 이름");

        // when
        verifyNoInteractions(assetRepository);
    }

    @Test
    @DisplayName("설명이 없으면 공유를 저장하지 않는다")
    void doesNotSaveShareWhenDescriptionIsMissing() {
        // given
        PortfolioShareCommand command = new PortfolioShareCommand(
                "  ", "PUBLIC", "BAND", true, true, true, true, false, false, false, false);

        // then
        assertThatThrownBy(() -> portfolioService.publish(1L, command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("공유 설명");

        // when
        verifyNoInteractions(shareRepository);
    }

    @Test
    @DisplayName("전체 공개 공유는 사용자명과 금액 구간을 저장한다")
    void publicShareStoresMaskedNameAndAmountBand() {
        // given
        PortfolioAssetJpaEntity asset = new PortfolioAssetJpaEntity(
                1L, "KODEX 미국S&P500", PortfolioAssetKind.ASSET, PortfolioAssetCategory.STOCK_ETF,
                20_000_000L, null, "미래에셋 ISA 110-123-456789", null, null, null, null);
        when(assetRepository.findByUserIdOrderByValuationAmountDescIdAsc(1L)).thenReturn(List.of(asset));
        when(snapshotRepository.findByUserIdAndYearMonth(eq(1L), any())).thenReturn(Optional.empty());
        when(snapshotRepository.findByUserIdOrderByYearMonthAsc(1L)).thenReturn(List.of());
        when(userPersistencePort.findById(1L)).thenReturn(Optional.of(User.builder().username("studio_kim").build()));

        // when
        portfolioService.publish(1L, new PortfolioShareCommand(
                "이번 달 생활비와 투자를 나눠 적었습니다.",
                "PUBLIC",
                "BAND",
                true,
                true,
                true,
                true,
                false,
                false,
                false,
                false));

        // given
        ArgumentCaptor<PortfolioShareJpaEntity> captor = ArgumentCaptor.forClass(PortfolioShareJpaEntity.class);

        // then
        verify(shareRepository).save(captor.capture());

        // when
        PortfolioShareJpaEntity saved = captor.getValue();

        // then
        assertThat(saved.getAuthorName()).isEqualTo("studio_kim");
        assertThat(saved.getMessage()).isEqualTo("이번 달 생활비와 투자를 나눠 적었습니다.");
        assertThat(saved.getVisibility()).isEqualTo("PUBLIC");
        assertThat(saved.getAmountLabel()).isEqualTo("약 2,000만원");
        assertThat(saved.getGoalLabel()).isEqualTo("1억 만들기");
        assertThat(saved.getPublicDetail()).doesNotContain("110-123-456789").doesNotContain("KODEX");
    }

    @Test
    @DisplayName("공개 목록은 공개 카드만 최신순으로 조회한다")
    void publicListReturnsOnlyPublicCardsLatestFirst() {
        // given
        when(shareRepository.findByVisibilityOrderByCreatedAtDescIdDesc(eq("PUBLIC"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(shareRepository.findByVisibilityAndCreatedAtGreaterThanEqual(eq("PUBLIC"), any())).thenReturn(List.of());

        // when

        // then
        assertThat(portfolioService.listPublicShares(0, 2).cards()).isEmpty();
        assertThat(portfolioService.listPublicShares(0, 2).hasNext()).isFalse();
    }

    @Test
    @DisplayName("등록된 자산이 없으면 이번 달 기록을 거절한다")
    void rejectsThisMonthRecordWhenNoAssetIsRegistered() {
        // given
        when(assetRepository.findByUserIdOrderByValuationAmountDescIdAsc(1L)).thenReturn(List.of());

        // when

        // then
        assertThatThrownBy(() -> portfolioService.recordCurrentMonth(1L, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("등록된 자산이 없어");
    }
}
