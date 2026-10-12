package com.sleekydz86.finsight.core.notification.service;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationServiceAlertTitleTest {

    @Test
    void 번역_제목을_알림_제목으로_쓴다() {
        News news = new News(
                1L,
                NewsProvider.MARKETAUX,
                LocalDateTime.parse("2026-10-07T09:00:00"),
                new Content("Fed holds rates", "body"),
                new Content("연준이 금리를 동결했다", "본문"),
                null,
                null);
        assertThat(NotificationService.alertTitle(news)).isEqualTo("연준이 금리를 동결했다");
    }

    @Test
    void 번역_제목이_없으면_원문을_쓴다() {
        News news = new News(
                2L,
                NewsProvider.MARKETAUX,
                LocalDateTime.parse("2026-10-07T09:00:00"),
                new Content("Fed holds rates", "body"),
                null,
                null,
                null);
        assertThat(NotificationService.alertTitle(news)).isEqualTo("Fed holds rates");
    }
}
