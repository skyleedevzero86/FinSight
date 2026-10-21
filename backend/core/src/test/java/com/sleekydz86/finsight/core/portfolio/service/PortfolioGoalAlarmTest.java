package com.sleekydz86.finsight.core.portfolio.service;

import com.sleekydz86.finsight.core.inbox.adapter.persistence.InboxNotificationJpaRepository;
import com.sleekydz86.finsight.core.inbox.domain.InboxCategory;
import com.sleekydz86.finsight.core.inbox.service.InboxService;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioAssetCategory;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioAssetKind;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioHolding;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.YearMonth;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PortfolioGoalAlarmTest {

    private final InboxNotificationJpaRepository repository = mock(InboxNotificationJpaRepository.class);
    private final InboxService inboxService = mock(InboxService.class);
    private final PortfolioGoalAlarm alarm = new PortfolioGoalAlarm(repository, inboxService);

    @Test
    @DisplayName("목표 순자산이면 활동 알림을 한 번 만든다")
    void createsOneActivityAlertWhenNetWorthReachesGoal() {
        // given
        when(repository.existsByRecipientUserIdAndRefTypeAndRefId(7L, PortfolioGoalAlarm.REF_TYPE, PortfolioSummary.GOAL_AMOUNT))
                .thenReturn(false);

        // when
        alarm.notifyIfReached(7L, summary(PortfolioSummary.GOAL_AMOUNT));

        // then
        verify(inboxService).createForUser(
                eq(7L),
                eq(InboxCategory.ADMIN),
                isNull(),
                eq("FinSight"),
                isNull(),
                eq("1억 목표를 달성했습니다."),
                eq("순자산이 목표 금액 100,000,000원에 도달했습니다."),
                eq("/myinfo/activity"),
                eq(PortfolioGoalAlarm.REF_TYPE),
                eq(PortfolioSummary.GOAL_AMOUNT));
    }

    @Test
    @DisplayName("목표 미만이거나 이미 알렸으면 다시 만들지 않는다")
    void doesNotCreateAgainWhenBelowGoalOrAlreadyAlerted() {
        // when
        alarm.notifyIfReached(7L, summary(PortfolioSummary.GOAL_AMOUNT - 1));

        // given
        when(repository.existsByRecipientUserIdAndRefTypeAndRefId(7L, PortfolioGoalAlarm.REF_TYPE, PortfolioSummary.GOAL_AMOUNT))
                .thenReturn(true);

        // when
        alarm.notifyIfReached(7L, summary(PortfolioSummary.GOAL_AMOUNT));

        // then
        verify(inboxService, never()).createForUser(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    private static PortfolioSummary summary(long amount) {
        return PortfolioSummaryCalculator.summarize(
                List.of(new PortfolioHolding(
                        1L, "예금", PortfolioAssetKind.ASSET, PortfolioAssetCategory.DEPOSIT, amount, null, null)),
                List.of(),
                YearMonth.of(2026, 10),
                false);
    }
}
