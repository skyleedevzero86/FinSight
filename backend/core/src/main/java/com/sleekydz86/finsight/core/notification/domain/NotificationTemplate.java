package com.sleekydz86.finsight.core.notification.domain;

import com.sleekydz86.finsight.core.user.domain.NotificationType;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.user.domain.User;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class NotificationTemplate {
    private final String title;
    private final String content;
    private final NotificationType type;
    private final NotificationChannel channel;
    private final Map<String, Object> variables;

    public static NotificationTemplate createNewsAlertTemplate(News news, User user, NotificationChannel channel) {
        String title = String.format("[FinSight] %s 관련 중요 뉴스",
                news.getAiOverView().getTargetCategories().stream()
                        .findFirst()
                        .map(category -> category.name())
                        .orElse("관심종목"));

        String content = String.format(
                "제목: %s\n\n" +
                        "요약: %s\n\n" +
                        "영향도: %s\n\n" +
                        "자세히 보기: %s",
                news.getOriginalContent().getTitle(),
                news.getAiOverView().getSummary(),
                news.getAiOverView().getImpactLevel(),
                news.getOriginalContent().getUrl()
        );

        return NotificationTemplate.builder()
                .title(title)
                .content(content)
                .type(NotificationType.NEWS_ALERT)
                .channel(channel)
                .build();
    }

    public static NotificationTemplate createSystemAlertTemplate(String title, String content, NotificationChannel channel) {
        return NotificationTemplate.builder()
                .title("[FinSight] " + title)
                .content(content)
                .type(NotificationType.SYSTEM_ALERT)
                .channel(channel)
                .build();
    }
}