package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.vo.AiOverview;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NewsNotificationServiceTest {

    @Mock
    private NotificationService notificationService;

    @Test
    void 감정점수가_0점7을_넘고_관심종목과_맞으면_알림한다() {
        News news = news(0.71);
        new NewsNotificationService(notificationService)
                .sendNotificationsForImportantNews(List.of(news));
        verify(notificationService).notifyUsersAboutNews(news);
    }

    @Test
    void 감정점수가_0점7_이하면_알림하지_않는다() {
        News news = news(0.7);
        new NewsNotificationService(notificationService)
                .sendNotificationsForImportantNews(List.of(news));
        verify(notificationService, never()).notifyUsersAboutNews(news);
    }

    @Test
    void 감정분석이_없으면_알림하지_않는다() {
        News news = new News(
                3L,
                NewsProvider.MARKETAUX,
                LocalDateTime.parse("2026-10-07T09:00:00"),
                new Content("Fed holds", "body"),
                null,
                null,
                null);
        new NewsNotificationService(notificationService)
                .sendNotificationsForImportantNews(List.of(news));
        verify(notificationService, never()).notifyUsersAboutNews(news);
    }

    private static News news(double score) {
        return new News(
                1L,
                NewsProvider.MARKETAUX,
                LocalDateTime.parse("2026-10-07T09:00:00"),
                new Content("Fed holds", "body"),
                new Content("연준이 금리를 동결했다", "번역 본문"),
                new AiOverview("요약", SentimentType.POSITIVE, score, List.of(TargetCategory.SPY)),
                null);
    }
}
