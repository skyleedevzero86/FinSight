package com.sleekydz86.finsight.core.portfolio.service;

import com.sleekydz86.finsight.core.inbox.adapter.persistence.InboxNotificationJpaRepository;
import com.sleekydz86.finsight.core.inbox.domain.InboxCategory;
import com.sleekydz86.finsight.core.inbox.service.InboxService;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioGoalAlarm {

    public static final String REF_TYPE = "PORTFOLIO_GOAL";

    private static final Logger log = LoggerFactory.getLogger(PortfolioGoalAlarm.class);

    private final InboxNotificationJpaRepository inboxNotificationJpaRepository;
    private final InboxService inboxService;

    public PortfolioGoalAlarm(
            InboxNotificationJpaRepository inboxNotificationJpaRepository,
            InboxService inboxService) {
        this.inboxNotificationJpaRepository = inboxNotificationJpaRepository;
        this.inboxService = inboxService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyIfReached(Long userId, PortfolioSummary summary) {
        if (!reached(userId, summary)) {
            return;
        }
        try {
            if (alreadySent(userId)) {
                return;
            }
            inboxService.createForUser(
                    userId,
                    InboxCategory.ADMIN,
                    null,
                    "FinSight",
                    null,
                    "1억 목표를 달성했습니다.",
                    "순자산이 목표 금액 100,000,000원에 도달했습니다.",
                    "/myinfo/activity",
                    REF_TYPE,
                    PortfolioSummary.GOAL_AMOUNT);
            log.info("1억 목표 달성 알림 등록 userId={}", userId);
        } catch (RuntimeException exception) {
            log.warn("1억 목표 알림 등록 실패 userId={}: {}", userId, exception.getMessage());
        }
    }

    static boolean reached(Long userId, PortfolioSummary summary) {
        return userId != null && summary != null && summary.netWorth() >= PortfolioSummary.GOAL_AMOUNT;
    }

    private boolean alreadySent(Long userId) {
        return inboxNotificationJpaRepository.existsByRecipientUserIdAndRefTypeAndRefId(
                userId, REF_TYPE, PortfolioSummary.GOAL_AMOUNT);
    }
}
