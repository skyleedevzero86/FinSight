package com.sleekydz86.finsight.core.notification.service;

import com.sleekydz86.finsight.core.notification.domain.Notification;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
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
        if (!emailEnabled) {
            log.debug("이메일 알림이 비활성화되어 있습니다.");
            return CompletableFuture.completedFuture(null);
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject(createNewsAlertSubject(news));

            String htmlContent = createNewsAlertHtmlContent(user, news);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("뉴스 알림 이메일 발송 성공 - 사용자: {}, 뉴스: {}",
                    user.getEmail(), news.getOriginalContent().getTitle());

            return CompletableFuture.completedFuture(null);

        } catch (MessagingException e) {
            log.error("뉴스 알림 이메일 발송 실패 - 사용자: {}, 오류: {}",
                    user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("이메일 발송 실패", e);
        }
    }

    @Async("notificationExecutor")
    @Retryable(retryFor = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public CompletableFuture<Void> sendSystemNotification(User user, Notification notification) {
        if (!emailEnabled) {
            log.debug("이메일 알림이 비활성화되어 있습니다.");
            return CompletableFuture.completedFuture(null);
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject(notification.getTitle());

            String htmlContent = createSystemNotificationHtmlContent(user, notification);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("시스템 알림 이메일 발송 성공 - 사용자: {}, 알림: {}",
                    user.getEmail(), notification.getTitle());

            return CompletableFuture.completedFuture(null);

        } catch (MessagingException e) {
            log.error("시스템 알림 이메일 발송 실패 - 사용자: {}, 오류: {}",
                    user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("이메일 발송 실패", e);
        }
    }

    @Async("notificationExecutor")
    public CompletableFuture<Void> sendWelcomeEmail(User user) {
        if (!emailEnabled) {
            return CompletableFuture.completedFuture(null);
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject(String.format("[%s] 회원가입을 환영합니다!", appName));

            String htmlContent = createWelcomeEmailContent(user);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("환영 이메일 발송 성공 - 사용자: {}", user.getEmail());

            return CompletableFuture.completedFuture(null);

        } catch (MessagingException e) {
            log.error("환영 이메일 발송 실패 - 사용자: {}, 오류: {}", user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("이메일 발송 실패", e);
        }
    }

    public void sendVerificationCodeEmail(
            String toEmail,
            String code,
            String purposeLabel,
            String requestedAtText,
            String requestLocation) {
        if (!emailEnabled) {
            throw new IllegalStateException("이메일 발송이 비활성화되어 있습니다. MAIL 설정을 확인해 주세요.");
        }
        if (fromEmail == null || fromEmail.isBlank()) {
            throw new IllegalStateException("네이버 메일 계정(MAIL_USERNAME)이 설정되어 있지 않습니다.");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("[%s] %s 코드", appName, purposeLabel));
            helper.setText(createVerificationCodeHtml(code, purposeLabel, requestedAtText, requestLocation), true);
            mailSender.send(message);
            log.info("검증 코드 이메일 발송 성공 - 수신: {}, 구분: {}", toEmail, purposeLabel);
        } catch (MessagingException e) {
            log.error("검증 코드 이메일 발송 실패 - 수신: {}, 오류: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("인증 메일 발송에 실패했습니다. 네이버 SMTP 설정을 확인해 주세요.", e);
        }
    }

    public void sendPasswordChangeReminder(User user, boolean warningOnly) {
        if (!emailEnabled) {
            log.debug("이메일 알림이 비활성화되어 비밀번호 안내를 건너뜁니다.");
            return;
        }
        if (fromEmail == null || fromEmail.isBlank() || user == null || user.getEmail() == null) {
            throw new IllegalStateException("비밀번호 안내 메일을 보낼 수 없습니다.");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            String subject = warningOnly
                    ? String.format(" [%s] 비밀번호 변경 예정 안내", appName)
                    : String.format("[%s] 비밀번호를 변경해 주세요", appName);
            helper.setSubject(subject.trim());
            helper.setText(createPasswordChangeReminderHtml(user, warningOnly), true);
            mailSender.send(message);
            log.info("비밀번호 변경 안내 메일 발송 성공 - 사용자: {}, 경고만: {}", user.getEmail(), warningOnly);
        } catch (MessagingException e) {
            log.error("비밀번호 변경 안내 메일 발송 실패 - 사용자: {}", user.getEmail(), e);
            throw new RuntimeException("비밀번호 변경 안내 메일 발송에 실패했습니다.", e);
        }
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
            String requestLocation) {
        String safeCode = escapeHtml(code);
        String safeWhen = escapeHtml(requestedAtText);
        String safeWhere = escapeHtml(requestLocation);
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
                    <div style="width:44px;height:44px;border-radius:12px;background:linear-gradient(145deg,#7c3aed 0%%,#6d28d9 100%%);margin-bottom:28px;"></div>
                    <h1 style="margin:0 0 16px;font-size:28px;line-height:1.3;font-weight:700;">검증 코드</h1>
                    <p style="margin:0 0 18px;font-size:15px;color:#222;">다음 인증 코드를 입력하세요:</p>
                    <p style="margin:0 0 18px;font-size:36px;letter-spacing:4px;font-weight:700;line-height:1.2;">%s</p>
                    <p style="margin:0 0 36px;font-size:14px;color:#333;">계정을 보호하기 위해 이 코드를 공유하지 마세요.</p>
                    <p style="margin:0 0 10px;font-size:15px;font-weight:700;">이걸 요청하지 않았나요?</p>
                    <p style="margin:0 0 8px;font-size:14px;line-height:1.6;color:#222;">
                      이 코드는 <strong>%s</strong>에 <strong>%s</strong>에서 요청되었습니다.
                    </p>
                    <p style="margin:0 0 28px;font-size:14px;color:#333;">이 요청을 하지 않으셨다면 이 이메일을 무시해도 됩니다.</p>
                    <div style="border-top:1px dashed #cfcfcf;padding-top:16px;">
                      <p style="margin:0;font-size:12px;color:#8a8a8a;">© %d %s · %s</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(safeCode, safeWhen, safeWhere, year, appName, escapeHtml(purposeLabel));
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

    private String createWelcomeEmailContent(User user) {
        try {
            String template = loadEmailTemplate("welcome.html");

            return template
                    .replace("{{userName}}", user.getUsername())
                    .replace("{{userEmail}}", user.getEmail())
                    .replace("{{frontendUrl}}", frontendUrl)
                    .replace("{{appName}}", appName)
                    .replace("{{currentTime}}",
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        } catch (IOException e) {
            log.error("환영 이메일 템플릿 로드 실패", e);
            return createFallbackWelcomeEmail(user);
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

    private String createFallbackWelcomeEmail(User user) {
        return String.format(
                """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="UTF-8">
                            <title>환영합니다!</title>
                            <style>
                                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                                .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                                .header { background: #7c3aed; color: white; padding: 20px; text-align: center; }
                                .content { padding: 20px; background: #f8f9fa; }
                                .footer { padding: 10px; text-align: center; font-size: 12px; color: #666; }
                                .button { background: #7c3aed; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <h1>%s에 오신 것을 환영합니다!</h1>
                                </div>
                                <div class="content">
                                    <h2>안녕하세요, %s님!</h2>
                                    <p>%s 회원이 되어주셔서 감사합니다.</p>
                                    <p>이제 맞춤형 금융 뉴스와 AI 분석을 받아보실 수 있습니다.</p>
                                    <p><a href="%s" class="button">시작하기</a></p>
                                </div>
                                <div class="footer">
                                    <p>© 2024 %s. All rights reserved.</p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                appName, user.getUsername(), appName, frontendUrl, appName);
    }

    @Async("notificationExecutor")
    @Retryable(retryFor = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public CompletableFuture<Void> sendRecoveryOtpEmail(User user, String subject, String content) {
        if (!emailEnabled) {
            log.debug("이메일 알림이 비활성화되어 있습니다.");
            return CompletableFuture.completedFuture(null);
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
            log.info("복구 OTP 이메일 발송 성공 - 사용자: {}", user.getEmail());

            return CompletableFuture.completedFuture(null);

        } catch (MessagingException e) {
            log.error("복구 OTP 이메일 발송 실패 - 사용자: {}, 오류: {}", user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("이메일 발송 실패", e);
        }
    }

    @Async("notificationExecutor")
    @Retryable(retryFor = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public CompletableFuture<Void> sendPasswordResetConfirmationEmail(User user, String subject, String content) {
        if (!emailEnabled) {
            log.debug("이메일 알림이 비활성화되어 있습니다.");
            return CompletableFuture.completedFuture(null);
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
            log.info("비밀번호 재설정 확인 이메일 발송 성공 - 사용자: {}", user.getEmail());

            return CompletableFuture.completedFuture(null);

        } catch (MessagingException e) {
            log.error("비밀번호 재설정 확인 이메일 발송 실패 - 사용자: {}, 오류: {}", user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("이메일 발송 실패", e);
        }
    }
}
