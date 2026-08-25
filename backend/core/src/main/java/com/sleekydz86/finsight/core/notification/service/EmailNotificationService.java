package com.sleekydz86.finsight.core.notification.service;

import com.sleekydz86.finsight.core.notification.domain.EmailMailPurpose;
import com.sleekydz86.finsight.core.notification.domain.EmailSendContext;
import com.sleekydz86.finsight.core.notification.domain.EmailSendContexts;
import com.sleekydz86.finsight.core.notification.domain.Notification;
import com.sleekydz86.finsight.core.notification.domain.RenderedEmailTemplate;
import com.sleekydz86.finsight.core.notification.domain.port.in.EmailLogCommandUseCase;
import com.sleekydz86.finsight.core.notification.domain.port.in.dto.EmailLogWriteCommand;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private static final String LOGO_CID = "finsight-logo";
    private static final String LOGO_CLASSPATH = "static/mail/finsight-logo.png";

    private final JavaMailSender mailSender;
    private final EmailLogCommandUseCase emailLogCommandUseCase;
    private final EmailTemplateQueryService emailTemplateQueryService;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.mail.from:${spring.mail.username:}}")
    private String fromEmail;

    @Value("${app.name:FinSight}")
    private String appName;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.notification.email.enabled:true}")
    private boolean emailEnabled;

    @Async("notificationExecutor")
    @Retryable(retryFor = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public CompletableFuture<Void> sendNewsAlert(User user, News news) {
        EmailSendContext context = EmailSendContexts.forUser(EmailMailPurpose.NEWS_ALERT, user.getId());
        String subject = createNewsAlertSubject(news);
        String htmlContent = createNewsAlertHtmlContent(user, news);
        sendAndLog(user.getEmail(), subject, htmlContent, "news-alert", context, true);
        return CompletableFuture.completedFuture(null);
    }

    @Async("notificationExecutor")
    @Retryable(retryFor = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public CompletableFuture<Void> sendSystemNotification(User user, Notification notification) {
        EmailSendContext context = EmailSendContexts.forUser(EmailMailPurpose.SYSTEM_NOTIFICATION, user.getId());
        String htmlContent = createSystemNotificationHtmlContent(user, notification);
        sendAndLog(user.getEmail(), notification.getTitle(), htmlContent, "system-notification", context, true);
        return CompletableFuture.completedFuture(null);
    }

    @Async("notificationExecutor")
    @Retryable(retryFor = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public CompletableFuture<Void> sendWelcomeEmail(User user) {
        sendWelcomeEmailSync(user);
        return CompletableFuture.completedFuture(null);
    }

    public void sendWelcomeEmailSync(User user) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalStateException("환영 메일을 보낼 수신자가 없습니다.");
        }

        String displayName = user.getNickname() != null && !user.getNickname().isBlank()
                ? user.getNickname()
                : user.getUsername();
        LocalDateTime registeredAt = user.getCreatedAt() != null ? user.getCreatedAt() : LocalDateTime.now();
        String registeredAtText = registeredAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("userName", escapeHtml(displayName));
        variables.put("userEmail", escapeHtml(user.getEmail()));
        variables.put("registeredAt", escapeHtml(registeredAtText));
        variables.put("frontendUrl", escapeHtml(frontendUrl));
        variables.put("appName", escapeHtml(appName));
        variables.put("year", String.valueOf(LocalDateTime.now().getYear()));

        RenderedEmailTemplate rendered = emailTemplateQueryService
                .renderActive(EmailTemplateSeedRunner.WELCOME, variables)
                .or(() -> emailTemplateQueryService.renderClasspathFallback(
                        "templates/email/welcome.html",
                        "[{{appName}}] 회원가입을 축하합니다",
                        variables))
                .orElseGet(() -> new RenderedEmailTemplate(
                        String.format("[%s] 회원가입을 축하합니다", appName),
                        createFallbackWelcomeEmail(user, registeredAtText)));

        EmailSendContext context = EmailSendContexts.forUser(EmailMailPurpose.WELCOME, user.getId());
        EmailSendContext masked = new EmailSendContext(
                context.purpose(),
                context.actorType(),
                user.getId(),
                context.actorUserId(),
                context.requestIp(),
                context.requestLocation(),
                context.userAgent(),
                context.relatedRef(),
                "회원가입 축하 메일 · 가입시각: " + registeredAtText);
        sendAndLog(user.getEmail(), rendered.subject(), rendered.htmlContent(), "welcome", masked, true);
    }

    public void sendVerificationCodeEmail(
            String toEmail,
            String code,
            String purposeLabel,
            String requestedAtText,
            String requestLocation,
            String challengeToken,
            EmailSendContext context) {
        if (!emailEnabled) {
            throw new IllegalStateException("이메일 발송이 비활성화되어 있습니다. MAIL 설정을 확인해 주세요.");
        }
        requireFromEmail();

        String disputeUrl = buildDisputeUrl(challengeToken);
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("code", escapeHtml(code));
        variables.put("purposeLabel", escapeHtml(purposeLabel));
        variables.put("requestedAt", escapeHtml(requestedAtText));
        variables.put("requestLocation", escapeHtml(requestLocation));
        variables.put("disputeUrl", escapeHtml(disputeUrl));
        variables.put("appName", escapeHtml(appName));
        variables.put("year", String.valueOf(LocalDateTime.now().getYear()));

        RenderedEmailTemplate rendered = emailTemplateQueryService
                .renderActive(EmailTemplateSeedRunner.VERIFICATION_CODE, variables)
                .or(() -> emailTemplateQueryService.renderClasspathFallback(
                        "templates/email/verification-code.html",
                        "[{{appName}}] {{purposeLabel}} 코드",
                        variables))
                .orElseGet(() -> new RenderedEmailTemplate(
                        String.format("[%s] %s 코드", appName, purposeLabel),
                        createVerificationCodeHtml(
                                code, purposeLabel, requestedAtText, requestLocation, disputeUrl)));

        EmailSendContext ctx = context != null
                ? context
                : EmailSendContexts.anonymous(
                        EmailMailPurpose.OTHER,
                        null,
                        requestLocation,
                        null,
                        null);
        EmailSendContext masked = new EmailSendContext(
                ctx.purpose(),
                ctx.actorType(),
                ctx.userId(),
                ctx.actorUserId(),
                ctx.requestIp(),
                ctx.requestLocation() != null ? ctx.requestLocation() : requestLocation,
                ctx.userAgent(),
                ctx.relatedRef(),
                "검증 코드 메일 · 구분: " + purposeLabel + " · 요청시각: " + requestedAtText);
        sendAndLog(toEmail, rendered.subject(), rendered.htmlContent(), "verification-code", masked, false);
    }

    public void sendAccountSuspendedNoticeEmail(User user, String purposeLabel, EmailSendContext context) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalStateException("계정 정지 안내 메일을 보낼 수 없습니다.");
        }
        requireFromEmail();

        String displayName = user.getNickname() != null && !user.getNickname().isBlank()
                ? user.getNickname()
                : user.getUsername();
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("userName", escapeHtml(displayName));
        variables.put("maskedUsername", escapeHtml(maskUsernameForMail(user.getUsername())));
        variables.put("maskedEmail", escapeHtml(maskEmailForMail(user.getEmail())));
        variables.put("purposeLabel", escapeHtml(purposeLabel != null ? purposeLabel : "이메일 인증"));
        variables.put("frontendUrl", escapeHtml(frontendUrl));
        variables.put("appName", escapeHtml(appName));
        variables.put("year", String.valueOf(LocalDateTime.now().getYear()));

        RenderedEmailTemplate rendered = emailTemplateQueryService
                .renderActive(EmailTemplateSeedRunner.ACCOUNT_SUSPENDED_NOTICE, variables)
                .or(() -> emailTemplateQueryService.renderClasspathFallback(
                        "templates/email/account-suspended-notice.html",
                        "[{{appName}}] 계정 정지 안내",
                        variables))
                .orElseGet(() -> new RenderedEmailTemplate(
                        String.format("[%s] 계정 정지 안내", appName),
                        createAccountSuspendedNoticeHtml(displayName, purposeLabel)));

        EmailSendContext ctx = context != null
                ? context
                : EmailSendContexts.forUser(EmailMailPurpose.ACCOUNT_SUSPENDED_NOTICE, user.getId());
        EmailSendContext masked = new EmailSendContext(
                EmailMailPurpose.ACCOUNT_SUSPENDED_NOTICE,
                ctx.actorType(),
                user.getId(),
                ctx.actorUserId(),
                ctx.requestIp(),
                ctx.requestLocation(),
                ctx.userAgent(),
                ctx.relatedRef(),
                "계정 정지 안내 · 원인: 요청하지 않은 인증 신고 · 구분: " + purposeLabel);
        sendAndLog(user.getEmail(), rendered.subject(), rendered.htmlContent(), "account-suspended-notice", masked, false);
    }

    private String buildDisputeUrl(String challengeToken) {
        String base = frontendUrl == null ? "" : frontendUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String token = challengeToken == null ? "" : challengeToken.trim();
        try {
            return base + "/verify/dispute/" + java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return base + "/verify/dispute/" + token;
        }
    }

    private static String maskUsernameForMail(String username) {
        if (username == null || username.isBlank()) {
            return "***";
        }
        String value = username.trim();
        int n = value.length();
        if (n == 1) {
            return "*";
        }
        if (n == 2) {
            return value.charAt(0) + "*";
        }
        if (n <= 4) {
            return value.charAt(0) + "*".repeat(n - 2) + value.charAt(n - 1);
        }
        return value.substring(0, 2) + "*".repeat(n - 4) + value.substring(n - 2);
    }

    private static String maskEmailForMail(String email) {
        if (email == null || email.isBlank()) {
            return "***";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) {
            return local.charAt(0) + "***" + domain;
        }
        return local.substring(0, 2) + "***" + domain;
    }

    public void sendPasswordChangeReminder(User user, boolean warningOnly) {
        if (!emailEnabled) {
            log.debug("이메일 알림이 비활성화되어 비밀번호 안내를 건너뜁니다.");
            return;
        }
        requireFromEmail();
        if (user == null || user.getEmail() == null) {
            throw new IllegalStateException("비밀번호 안내 메일을 보낼 수 없습니다.");
        }

        String subject = warningOnly
                ? String.format("[%s] 비밀번호 변경 예정 안내", appName)
                : String.format("[%s] 비밀번호를 변경해 주세요", appName);
        String htmlContent = createPasswordChangeReminderHtml(user, warningOnly);
        EmailSendContext context = EmailSendContexts.forUser(EmailMailPurpose.PASSWORD_CHANGE_REMINDER, user.getId());
        sendAndLog(user.getEmail(), subject.trim(), htmlContent, "password-change-reminder", context, false);
    }

    @Async("notificationExecutor")
    @Retryable(retryFor = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public CompletableFuture<Void> sendRecoveryOtpEmail(User user, String subject, String content) {
        return sendRecoveryOtpEmail(user, subject, content, null);
    }

    @Async("notificationExecutor")
    @Retryable(retryFor = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public CompletableFuture<Void> sendRecoveryOtpEmail(
            User user, String subject, String content, EmailSendContext context) {
        EmailSendContext ctx = context != null
                ? context
                : EmailSendContexts.forUser(EmailMailPurpose.ACCOUNT_RECOVERY_OTP, user.getId());
        EmailSendContext masked = new EmailSendContext(
                EmailMailPurpose.ACCOUNT_RECOVERY_OTP,
                ctx.actorType(),
                user.getId(),
                ctx.actorUserId(),
                ctx.requestIp(),
                ctx.requestLocation(),
                ctx.userAgent(),
                ctx.relatedRef(),
                "계정 복구 OTP 메일 (코드 본문은 이력에 저장하지 않음)");
        sendAndLog(user.getEmail(), subject, content, "account-recovery-otp", masked, true);
        return CompletableFuture.completedFuture(null);
    }

    @Async("notificationExecutor")
    @Retryable(retryFor = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public CompletableFuture<Void> sendPasswordResetConfirmationEmail(User user, String subject, String content) {
        return sendPasswordResetConfirmationEmail(user, subject, content, null);
    }

    @Async("notificationExecutor")
    @Retryable(retryFor = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public CompletableFuture<Void> sendPasswordResetConfirmationEmail(
            User user, String subject, String content, EmailSendContext context) {
        EmailSendContext ctx = context != null
                ? context
                : EmailSendContexts.forUser(EmailMailPurpose.PASSWORD_RESET_CONFIRMATION, user.getId());
        sendAndLog(user.getEmail(), subject, content, "password-reset-confirmation", ctx, true);
        return CompletableFuture.completedFuture(null);
    }

    private void sendAndLog(
            String toEmail,
            String subject,
            String htmlContent,
            String templateType,
            EmailSendContext context,
            boolean skipWhenDisabled) {
        if (!emailEnabled) {
            log.debug("이메일 알림이 비활성화되어 있습니다. template={}", templateType);
            if (skipWhenDisabled) {
                return;
            }
            throw new IllegalStateException("이메일 발송이 비활성화되어 있습니다. MAIL 설정을 확인해 주세요.");
        }
        requireFromEmail();
        String from = resolveFromAddress();

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            attachInlineLogoIfNeeded(helper, htmlContent);
            mailSender.send(message);

            emailLogCommandUseCase.recordSuccess(new EmailLogWriteCommand(
                    toEmail, subject, from, templateType, htmlContent, context, null));
            log.info("이메일 발송 성공 - to={}, purpose={}, template={}",
                    toEmail,
                    context != null ? context.purpose() : null,
                    templateType);
        } catch (MessagingException | RuntimeException e) {
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            emailLogCommandUseCase.recordFailure(new EmailLogWriteCommand(
                    toEmail, subject, from, templateType, htmlContent, context, message));
            log.error("이메일 발송 실패 - to={}, template={}, error={}", toEmail, templateType, message, e);
            throw new RuntimeException("이메일 발송 실패: " + message, e);
        }
    }

    private void attachInlineLogoIfNeeded(MimeMessageHelper helper, String htmlContent) throws MessagingException {
        if (htmlContent == null || !htmlContent.contains("cid:" + LOGO_CID)) {
            return;
        }
        ClassPathResource logo = new ClassPathResource(LOGO_CLASSPATH);
        if (!logo.exists()) {
            log.warn("메일 로고 리소스 없음: {}", LOGO_CLASSPATH);
            return;
        }
        helper.addInline(LOGO_CID, logo, "image/png");
    }

    private void requireFromEmail() {
        if (resolveFromAddress().isBlank()) {
            throw new IllegalStateException("네이버 메일 계정(MAIL_USERNAME / app.mail.from)이 설정되어 있지 않습니다.");
        }
    }

    private String resolveFromAddress() {
        String from = fromEmail != null ? fromEmail.trim() : "";
        if (!from.isBlank()) {
            return from.contains("@") ? from : from + "@naver.com";
        }
        String user = mailUsername != null ? mailUsername.trim() : "";
        if (user.isBlank()) {
            return "";
        }
        return user.contains("@") ? user : user + "@naver.com";
    }

    private String createPasswordChangeReminderHtml(User user, boolean warningOnly) {
        String name = escapeHtml(user.getNickname() != null ? user.getNickname() : user.getUsername());
        String changeUrl = escapeHtml(frontendUrl + "/my?password=required");
        long daysLeft = user.daysUntilPasswordExpiry();
        int year = LocalDateTime.now().getYear();
        String title = warningOnly ? "비밀번호 변경 시기가 다가왔습니다" : "비밀번호를 변경해 주세요";
        String body = warningOnly
                ? String.format("보안을 위해 사이트 비밀번호는 90일마다 변경해야 합니다. 남은 기간은 약 %d일입니다.", daysLeft)
                : "사이트 비밀번호를 마지막으로 변경한 지 90일이 지났습니다. 로그인 후 내정보에서 새 비밀번호로 바꿔 주세요.";
        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>%s</title>
                </head>
                <body style="margin:0;padding:0;background:#ffffff;font-family:'Apple SD Gothic Neo','Malgun Gothic',Arial,sans-serif;color:#111111;">
                  <div style="max-width:560px;margin:0 auto;padding:32px 28px 24px;">
                    <h1 style="margin:0 0 16px;font-size:24px;line-height:1.3;font-weight:700;">%s</h1>
                    <p style="margin:0 0 12px;font-size:15px;color:#222;">안녕하세요, %s님.</p>
                    <p style="margin:0 0 24px;font-size:14px;line-height:1.7;color:#333;">%s</p>
                    <p style="margin:0 0 28px;">
                      <a href="%s" style="display:inline-block;background:#111111;color:#ffffff;text-decoration:none;padding:12px 18px;border-radius:6px;font-size:14px;font-weight:600;">비밀번호 변경하기</a>
                    </p>
                    <div style="border-top:1px dashed #cfcfcf;padding-top:16px;">
                      <p style="margin:0;font-size:12px;color:#8a8a8a;">© %d %s</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(title, title, name, body, changeUrl, year, escapeHtml(appName));
    }

    private String createVerificationCodeHtml(
            String code,
            String purposeLabel,
            String requestedAtText,
            String requestLocation,
            String disputeUrl) {
        String safeCode = escapeHtml(code);
        String safeWhen = escapeHtml(requestedAtText);
        String safeWhere = escapeHtml(requestLocation);
        String safeDispute = escapeHtml(disputeUrl);
        int year = LocalDateTime.now().getYear();
        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>검증 코드</title>
                </head>
                <body style="margin:0;padding:0;background:#ffffff;font-family:'Apple SD Gothic Neo','Malgun Gothic',Arial,sans-serif;color:#111111;">
                  <div style="max-width:560px;margin:0 auto;padding:32px 28px 24px;">
                    <img src="cid:finsight-logo" alt="FinSight" width="140" style="display:block;width:140px;height:auto;margin:0 0 28px 0;border:0;" />
                    <h1 style="margin:0 0 16px;font-size:28px;line-height:1.3;font-weight:700;">검증 코드</h1>
                    <p style="margin:0 0 18px;font-size:15px;color:#222;">다음 인증 코드를 입력하세요:</p>
                    <p style="margin:0 0 18px;font-size:36px;letter-spacing:4px;font-weight:700;line-height:1.2;">%s</p>
                    <p style="margin:0 0 36px;font-size:14px;color:#333;">계정을 보호하기 위해 이 코드를 공유하지 마세요.</p>
                    <p style="margin:0 0 10px;font-size:15px;font-weight:700;">
                      <a href="%s" style="color:#111111;text-decoration:underline;">이걸 요청하지 않았나요?</a>
                    </p>
                    <p style="margin:0 0 8px;font-size:14px;line-height:1.6;color:#222;">
                      이 코드는 <strong>%s</strong>에 <strong>%s</strong>에서 요청되었습니다.
                    </p>
                    <p style="margin:0 0 28px;font-size:14px;color:#333;">이 요청을 하지 않으셨다면 위 링크를 눌러 계정을 보호해 주세요.</p>
                    <div style="border-top:1px dashed #cfcfcf;padding-top:16px;">
                      <p style="margin:0;font-size:12px;color:#8a8a8a;">© %d %s · %s</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(safeCode, safeDispute, safeWhen, safeWhere, year, appName, escapeHtml(purposeLabel));
    }

    private String createAccountSuspendedNoticeHtml(String userName, String purposeLabel) {
        String safeName = escapeHtml(userName);
        String safePurpose = escapeHtml(purposeLabel != null ? purposeLabel : "이메일 인증");
        int year = LocalDateTime.now().getYear();
        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>계정 정지 안내</title>
                </head>
                <body style="margin:0;padding:0;background:#ffffff;font-family:'Apple SD Gothic Neo','Malgun Gothic',Arial,sans-serif;color:#111111;">
                  <div style="max-width:560px;margin:0 auto;padding:32px 28px 24px;">
                    <img src="cid:finsight-logo" alt="FinSight" width="140" style="display:block;width:140px;height:auto;margin:0 0 28px 0;border:0;" />
                    <h1 style="margin:0 0 16px;font-size:26px;line-height:1.3;font-weight:700;">계정 정지 안내</h1>
                    <p style="margin:0 0 14px;font-size:15px;line-height:1.6;color:#222;">%s님, 안녕하세요.</p>
                    <p style="margin:0 0 14px;font-size:15px;line-height:1.6;color:#222;">
                      요청하지 않은 <strong>%s</strong> 인증 신고로 계정이 정지되었습니다.
                    </p>
                    <p style="margin:0 0 28px;font-size:15px;line-height:1.6;color:#222;">
                      계정 복구가 필요하시면 관리자에게 문의해 주세요.
                    </p>
                    <div style="border-top:1px dashed #cfcfcf;padding-top:16px;">
                      <p style="margin:0;font-size:12px;color:#8a8a8a;">© %d %s</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(safeName, safePurpose, year, appName);
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String createNewsAlertSubject(News news) {
        List<TargetCategory> categories = news.getAiOverView().getTargetCategories();
        String categoryText = categories.isEmpty() ? "관심종목" : categories.get(0).name();
        return String.format("[%s] %s 관련 중요 뉴스 알림", appName, categoryText);
    }

    private String createNewsAlertHtmlContent(User user, News news) {
        try {
            String template = loadEmailTemplate("news-alert.html");

            return template
                    .replace("{{userName}}", user.getUsername())
                    .replace("{{newsTitle}}", news.getOriginalContent().getTitle())
                    .replace("{{newsContent}}", truncateContent(news.getOriginalContent().getContent(), 200))
                    .replace("{{newsUrl}}", news.getNewsMeta().getSourceUrl())
                    .replace("{{frontendUrl}}", frontendUrl)
                    .replace("{{appName}}", appName)
                    .replace("{{currentTime}}",
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .replace("{{categories}}", formatCategories(news.getAiOverView().getTargetCategories()));

        } catch (IOException e) {
            log.error("뉴스 알림 이메일 템플릿 로드 실패", e);
            return createFallbackNewsAlert(user, news);
        }
    }

    private String createSystemNotificationHtmlContent(User user, Notification notification) {
        try {
            String template = loadEmailTemplate("system-notification.html");

            return template
                    .replace("{{userName}}", user.getUsername())
                    .replace("{{notificationTitle}}", notification.getTitle())
                    .replace("{{notificationContent}}", notification.getContent())
                    .replace("{{frontendUrl}}", frontendUrl)
                    .replace("{{appName}}", appName)
                    .replace("{{currentTime}}",
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        } catch (IOException e) {
            log.error("시스템 알림 이메일 템플릿 로드 실패", e);
            return createFallbackSystemNotification(user, notification);
        }
    }

    private String loadEmailTemplate(String templateName) throws IOException {
        ClassPathResource resource = new ClassPathResource("templates/email/" + templateName);
        if (!resource.exists()) {
            throw new IOException("이메일 템플릿을 찾을 수 없습니다: " + templateName);
        }
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    private String truncateContent(String content, int maxLength) {
        if (content == null)
            return "";
        if (content.length() <= maxLength)
            return content;
        return content.substring(0, maxLength) + "...";
    }

    private String formatCategories(List<TargetCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return "일반";
        }
        return categories.stream()
                .map(TargetCategory::name)
                .reduce((a, b) -> a + ", " + b)
                .orElse("일반");
    }

    private String createFallbackNewsAlert(User user, News news) {
        return String.format(
                """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="UTF-8">
                            <title>뉴스 알림</title>
                            <style>
                                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                                .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                                .header { background: #2563eb; color: white; padding: 20px; text-align: center; }
                                .content { padding: 20px; background: #f8f9fa; }
                                .footer { padding: 10px; text-align: center; font-size: 12px; color: #666; }
                                .button { background: #2563eb; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <h1>%s</h1>
                                </div>
                                <div class="content">
                                    <h2>안녕하세요, %s님!</h2>
                                    <p>관심 있으실만한 뉴스가 있어 알려드립니다.</p>
                                    <h3>%s</h3>
                                    <p>%s</p>
                                    <p><a href="%s" class="button">뉴스 보기</a></p>
                                </div>
                                <div class="footer">
                                    <p>© 2024 %s. All rights reserved.</p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                appName, user.getUsername(), news.getOriginalContent().getTitle(),
                truncateContent(news.getOriginalContent().getContent(), 200),
                news.getNewsMeta().getSourceUrl(), appName);
    }

    private String createFallbackSystemNotification(User user, Notification notification) {
        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>시스템 알림</title>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #059669; color: white; padding: 20px; text-align: center; }
                        .content { padding: 20px; background: #f8f9fa; }
                        .footer { padding: 10px; text-align: center; font-size: 12px; color: #666; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>%s 알림</h1>
                        </div>
                        <div class="content">
                            <h2>안녕하세요, %s님!</h2>
                            <h3>%s</h3>
                            <p>%s</p>
                        </div>
                        <div class="footer">
                            <p>© 2024 %s. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """, appName, user.getUsername(), notification.getTitle(),
                notification.getContent(), appName);
    }

    private String createFallbackWelcomeEmail(User user, String registeredAtText) {
        String name = escapeHtml(user.getNickname() != null && !user.getNickname().isBlank()
                ? user.getNickname()
                : user.getUsername());
        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head><meta charset="UTF-8"><title>회원가입 축하</title></head>
                <body style="margin:0;padding:0;background:#ffffff;font-family:'Apple SD Gothic Neo','Malgun Gothic',Arial,sans-serif;color:#111111;">
                  <div style="max-width:560px;margin:0 auto;padding:32px 28px 24px;">
                    <img src="cid:finsight-logo" alt="FinSight" width="140" style="display:block;width:140px;height:auto;margin:0 0 28px 0;border:0;" />
                    <h1 style="margin:0 0 16px;font-size:28px;font-weight:700;">가입을 축하합니다</h1>
                    <p style="margin:0 0 18px;font-size:15px;line-height:1.6;">안녕하세요, <strong>%s</strong>님.</p>
                    <p style="margin:0 0 18px;font-size:14px;line-height:1.6;">가입 이메일: <strong>%s</strong><br/>가입 시각: <strong>%s</strong></p>
                    <p style="margin:0 0 28px;font-size:15px;line-height:1.6;">%s 회원이 되어 주셔서 감사합니다.</p>
                    <p style="margin:0 0 36px;"><a href="%s" style="display:inline-block;padding:12px 22px;background:#111111;color:#ffffff;text-decoration:none;font-size:14px;font-weight:700;">시작하기</a></p>
                    <p style="margin:0;font-size:12px;color:#8a8a8a;">© %d %s</p>
                  </div>
                </body>
                </html>
                """.formatted(
                name,
                escapeHtml(user.getEmail()),
                escapeHtml(registeredAtText),
                escapeHtml(appName),
                escapeHtml(frontendUrl),
                LocalDateTime.now().getYear(),
                escapeHtml(appName));
    }
}
