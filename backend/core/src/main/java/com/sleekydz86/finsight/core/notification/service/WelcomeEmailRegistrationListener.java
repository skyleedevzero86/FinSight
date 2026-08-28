package com.sleekydz86.finsight.core.notification.service;

import com.sleekydz86.finsight.core.user.domain.event.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class WelcomeEmailRegistrationListener {

    private static final Logger log = LoggerFactory.getLogger(WelcomeEmailRegistrationListener.class);

    private final WelcomeEmailDispatchService welcomeEmailDispatchService;

    public WelcomeEmailRegistrationListener(WelcomeEmailDispatchService welcomeEmailDispatchService) {
        this.welcomeEmailDispatchService = welcomeEmailDispatchService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        try {
            welcomeEmailDispatchService.enqueue(event.userId(), event.registeredAt());
        } catch (Exception e) {
            log.error("회원가입 축하 메일 예약 실패: userId={}, error={}",
                    event.userId(), e.getMessage(), e);
        }
    }
}
