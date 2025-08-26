package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NewsNotificationService {

    private static final Logger log = LoggerFactory.getLogger(NewsNotificationService.class);

    private final NotificationService notificationService;

    public NewsNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void sendNotificationsForImportantNews(List<News> newses) {
        try {
            List<News> importantNews = newses.stream()
                    .filter(news -> news.getAiOverView() != null &&
                            news.getAiOverView().getSentimentScore() > 0.7)
                    .collect(Collectors.toList());

            if (!importantNews.isEmpty()) {
                log.info("중요 뉴스 {} 건에 대한 알림 발송 시작", importantNews.size());
                importantNews.forEach(news -> {
                    try {
                        notificationService.notifyUsersAboutNews(news);
                    } catch (Exception e) {
                        log.error("알림 발송 실패: {}", news.getId(), e);
                    }
                });
            }
        } catch (Exception e) {
            log.error("알림 발송 처리 중 오류", e);
        }
    }
}