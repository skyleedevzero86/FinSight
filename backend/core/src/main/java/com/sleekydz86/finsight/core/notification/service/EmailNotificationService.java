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

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name:FinSight}")
    private String appName;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.notification.email.enabled:true}")
    private boolean emailEnabled;

    @Async("notificationExecutor")
    @Retryable(retryFor = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
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
    @Retryable(retryFor = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
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

    // 템플릿 로드 실패 시 fallback HTML
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
    @Retryable(retryFor = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
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
    @Retryable(retryFor = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
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