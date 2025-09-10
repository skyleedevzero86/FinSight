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
                news.getAiOverView().getImpactLevel().getDescription(),
                getNewsUrl(news)
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

    public static NotificationTemplate createPriceAlertTemplate(String symbol, double price, double changePercent, NotificationChannel channel) {
        String title = String.format("[FinSight] %s 가격 알림", symbol);
        String content = String.format(
                "%s의 가격이 %.2f원으로 %.2f%% 변동했습니다.",
                symbol, price, changePercent
        );

        return NotificationTemplate.builder()
                .title(title)
                .content(content)
                .type(NotificationType.PRICE_ALERT)
                .channel(channel)
                .build();
    }

    public static NotificationTemplate createMarketSummaryTemplate(String summary, NotificationChannel channel) {
        return NotificationTemplate.builder()
                .title("[FinSight] 오늘의 시장 요약")
                .content(summary)
                .type(NotificationType.MARKET_SUMMARY)
                .channel(channel)
                .build();
    }

    public static NotificationTemplate createAccountAlertTemplate(String title, String content, NotificationChannel channel) {
        return NotificationTemplate.builder()
                .title("[FinSight] " + title)
                .content(content)
                .type(NotificationType.ACCOUNT_ALERT)
                .channel(channel)
                .build();
    }

    private static String getNewsUrl(News news) {

        if (news.getOriginalContent().getUrl() != null && !news.getOriginalContent().getUrl().isEmpty()) {
            return news.getOriginalContent().getUrl();
        }

        if (news.getNewsMeta() != null && news.getNewsMeta().getSourceUrl() != null) {
            return news.getNewsMeta().getSourceUrl();
        }

        return "URL 정보 없음";
    }
}