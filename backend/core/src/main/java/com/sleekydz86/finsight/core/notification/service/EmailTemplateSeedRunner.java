package com.sleekydz86.finsight.core.notification.service;

import com.sleekydz86.finsight.core.notification.domain.EmailTemplate;
import com.sleekydz86.finsight.core.notification.domain.port.out.EmailTemplatePersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Component
@Order(40)
public class EmailTemplateSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EmailTemplateSeedRunner.class);
    public static final String VERIFICATION_CODE = "verification-code";
    public static final String WELCOME = "welcome";
    public static final String ACCOUNT_SUSPENDED_NOTICE = "account-suspended-notice";

    private final EmailTemplatePersistencePort emailTemplatePersistencePort;

    public EmailTemplateSeedRunner(EmailTemplatePersistencePort emailTemplatePersistencePort) {
        this.emailTemplatePersistencePort = emailTemplatePersistencePort;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedVerificationCode();
        seedWelcome();
        seedAccountSuspendedNotice();
    }

    private void seedVerificationCode() {
        try {
            ClassPathResource resource = new ClassPathResource("templates/email/verification-code.html");
            String html = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            EmailTemplate template = emailTemplatePersistencePort.findByName(VERIFICATION_CODE)
                    .orElseGet(EmailTemplate::new);
            boolean created = template.getId() == null;
            template.setName(VERIFICATION_CODE);
            template.setSubject("[{{appName}}] {{purposeLabel}} 코드");
            template.setHtmlContent(html);
            template.setTextContent("검증 코드: {{code}}");
            template.setTemplateVariables(
                    "[\"code\",\"purposeLabel\",\"requestedAt\",\"requestLocation\",\"disputeUrl\",\"appName\",\"year\"]");
            template.setActive(true);
            if (created) {
                template.setCreatedAt(LocalDateTime.now());
            }
            template.setUpdatedAt(LocalDateTime.now());
            emailTemplatePersistencePort.save(template);
            log.info("email_templates {} 완료: {}", created ? "시드" : "갱신", VERIFICATION_CODE);
        } catch (Exception e) {
            log.warn("email_templates 시드 실패: {}", e.getMessage());
        }
    }

    private void seedWelcome() {
        try {
            ClassPathResource resource = new ClassPathResource("templates/email/welcome.html");
            String html = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            EmailTemplate template = emailTemplatePersistencePort.findByName(WELCOME)
                    .orElseGet(EmailTemplate::new);
            boolean created = template.getId() == null;
            template.setName(WELCOME);
            template.setSubject("[{{appName}}] 회원가입을 축하합니다");
            template.setHtmlContent(html);
            template.setTextContent("{{userName}}님, {{appName}} 가입을 축하합니다.");
            template.setTemplateVariables(
                    "[\"userName\",\"userEmail\",\"registeredAt\",\"frontendUrl\",\"appName\",\"year\"]");
            template.setActive(true);
            if (created) {
                template.setCreatedAt(LocalDateTime.now());
            }
            template.setUpdatedAt(LocalDateTime.now());
            emailTemplatePersistencePort.save(template);
            log.info("email_templates {} 완료: {}", created ? "시드" : "갱신", WELCOME);
        } catch (Exception e) {
            log.warn("email_templates welcome 시드 실패: {}", e.getMessage());
        }
    }

    private void seedAccountSuspendedNotice() {
        try {
            ClassPathResource resource = new ClassPathResource("templates/email/account-suspended-notice.html");
            String html = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            EmailTemplate template = emailTemplatePersistencePort.findByName(ACCOUNT_SUSPENDED_NOTICE)
                    .orElseGet(EmailTemplate::new);
            boolean created = template.getId() == null;
            template.setName(ACCOUNT_SUSPENDED_NOTICE);
            template.setSubject("[{{appName}}] 계정 정지 안내");
            template.setHtmlContent(html);
            template.setTextContent("요청하지 않은 인증 신고로 계정이 정지되었습니다. 관리자에게 문의해 주세요.");
            template.setTemplateVariables(
                    "[\"userName\",\"maskedUsername\",\"maskedEmail\",\"purposeLabel\",\"frontendUrl\",\"appName\",\"year\"]");
            template.setActive(true);
            if (created) {
                template.setCreatedAt(LocalDateTime.now());
            }
            template.setUpdatedAt(LocalDateTime.now());
            emailTemplatePersistencePort.save(template);
            log.info("email_templates {} 완료: {}", created ? "시드" : "갱신", ACCOUNT_SUSPENDED_NOTICE);
        } catch (Exception e) {
            log.warn("email_templates account-suspended-notice 시드 실패: {}", e.getMessage());
        }
    }
}
