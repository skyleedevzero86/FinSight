package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.vo.AiOverview;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("감정점수가 0점7을 넘고 관심종목과 맞으면 알림한다")
    void notifiesWhenSentimentAboveZeroPointSevenMatchesWatchlist() {
        // given
        News news = news(0.71);

        // when
        new NewsNotificationService(notificationService)
                .sendNotificationsForImportantNews(List.of(news));

        // then
        verify(notificationService).notifyUsersAboutNews(news);
    }

    @Test
    @DisplayName("감정점수가 0점7 이하면 알림하지 않는다")
    void doesNotNotifyWhenSentimentIsAtMostZeroPointSeven() {
        // given
        News news = news(0.7);

        // when
        new NewsNotificationService(notificationService)
                .sendNotificationsForImportantNews(List.of(news));

        // then
        verify(notificationService, never()).notifyUsersAboutNews(news);
    }

    @Test
    @DisplayName("감정분석이 없으면 알림하지 않는다")
    void doesNotNotifyWithoutSentimentAnalysis() {
        // given
        News news = new News(
                3L,
                NewsProvider.MARKETAUX,
                LocalDateTime.parse("2026-10-07T09:00:00"),
                new Content("Fed holds", "body"),
                null,
                null,
                null);

        // when
        new NewsNotificationService(notificationService)
                .sendNotificationsForImportantNews(List.of(news));

        // then
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
