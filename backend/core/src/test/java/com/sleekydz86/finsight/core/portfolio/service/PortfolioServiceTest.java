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
    void 이름이_비어_있으면_자산을_저장하지_않는다() {
        PortfolioAssetCommand command = new PortfolioAssetCommand("  ", "ASSET", "CASH", 10_000L, null, null, null, null, null, null);

        assertThatThrownBy(() -> portfolioService.register(1L, command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("자산 이름");
        verifyNoInteractions(assetRepository);
    }

    @Test
    void 설명이_없으면_공유를_저장하지_않는다() {
        PortfolioShareCommand command = new PortfolioShareCommand(
                "  ", "PUBLIC", "BAND", true, true, true, true, false, false, false, false);

        assertThatThrownBy(() -> portfolioService.publish(1L, command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("공유 설명");
        verifyNoInteractions(shareRepository);
    }

    @Test
    void 전체_공개_공유는_사용자명과_금액_구간을_저장한다() {
        PortfolioAssetJpaEntity asset = new PortfolioAssetJpaEntity(
                1L, "KODEX 미국S&P500", PortfolioAssetKind.ASSET, PortfolioAssetCategory.STOCK_ETF,
                20_000_000L, null, "미래에셋 ISA 110-123-456789", null, null, null, null);
        when(assetRepository.findByUserIdOrderByValuationAmountDescIdAsc(1L)).thenReturn(List.of(asset));
        when(snapshotRepository.findByUserIdAndYearMonth(eq(1L), any())).thenReturn(Optional.empty());
        when(snapshotRepository.findByUserIdOrderByYearMonthAsc(1L)).thenReturn(List.of());
        when(userPersistencePort.findById(1L)).thenReturn(Optional.of(User.builder().username("studio_kim").build()));

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

        ArgumentCaptor<PortfolioShareJpaEntity> captor = ArgumentCaptor.forClass(PortfolioShareJpaEntity.class);
        verify(shareRepository).save(captor.capture());
        PortfolioShareJpaEntity saved = captor.getValue();
        assertThat(saved.getAuthorName()).isEqualTo("studio_kim");
        assertThat(saved.getMessage()).isEqualTo("이번 달 생활비와 투자를 나눠 적었습니다.");
        assertThat(saved.getVisibility()).isEqualTo("PUBLIC");
        assertThat(saved.getAmountLabel()).isEqualTo("약 2,000만원");
        assertThat(saved.getGoalLabel()).isEqualTo("1억 만들기");
        assertThat(saved.getPublicDetail()).doesNotContain("110-123-456789").doesNotContain("KODEX");
    }

    @Test
    void 공개_목록은_공개_카드만_최신순으로_조회한다() {
        when(shareRepository.findByVisibilityOrderByCreatedAtDescIdDesc(eq("PUBLIC"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(shareRepository.findByVisibilityAndCreatedAtGreaterThanEqual(eq("PUBLIC"), any())).thenReturn(List.of());

        assertThat(portfolioService.listPublicShares(0, 2).cards()).isEmpty();
        assertThat(portfolioService.listPublicShares(0, 2).hasNext()).isFalse();
    }

    @Test
    void 등록된_자산이_없으면_이번_달_기록을_거절한다() {
        when(assetRepository.findByUserIdOrderByValuationAmountDescIdAsc(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> portfolioService.recordCurrentMonth(1L, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("등록된 자산이 없어");
    }
}
