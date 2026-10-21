package com.sleekydz86.finsight.core.notification.service;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationServiceAlertTitleTest {

    @Test
    @DisplayName("번역 제목을 알림 제목으로 쓴다")
    void usesTranslatedTitleAsAlertTitle() {
        // given
        News news = new News(
                1L,
                NewsProvider.MARKETAUX,
                LocalDateTime.parse("2026-10-07T09:00:00"),
                new Content("Fed holds rates", "body"),
                new Content("연준이 금리를 동결했다", "본문"),
                null,
                null);

        // when

        // then
        assertThat(NotificationService.alertTitle(news)).isEqualTo("연준이 금리를 동결했다");
    }

    @Test
    @DisplayName("번역 제목이 없으면 원문을 쓴다")
    void usesOriginalTitleWhenTranslationIsMissing() {
        // given
        News news = new News(
                2L,
                NewsProvider.MARKETAUX,
                LocalDateTime.parse("2026-10-07T09:00:00"),
                new Content("Fed holds rates", "body"),
                null,
                null,
                null);

        // when

        // then
        assertThat(NotificationService.alertTitle(news)).isEqualTo("Fed holds rates");
    }
}
