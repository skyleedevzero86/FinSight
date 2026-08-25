package com.sleekydz86.finsight.core.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WelcomeEmailScheduler {

    private static final Logger log = LoggerFactory.getLogger(WelcomeEmailScheduler.class);

    private final WelcomeEmailDispatchService welcomeEmailDispatchService;

    public WelcomeEmailScheduler(WelcomeEmailDispatchService welcomeEmailDispatchService) {
        this.welcomeEmailDispatchService = welcomeEmailDispatchService;
    }

    @Scheduled(fixedDelayString = "${app.mail.welcome.poll-interval-ms:30000}")
    public void pollWelcomeEmails() {
        try {
            welcomeEmailDispatchService.processDueJobs();
        } catch (Exception e) {
            log.error("회원가입 축하 메일 스케줄 처리 실패: {}", e.getMessage(), e);
        }
    }
}
