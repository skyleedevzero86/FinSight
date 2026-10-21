package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.vo.AiOverview;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.news.domain.vo.NewsMeta;
import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsAiAnalysisRequesterPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsAiProcessingServiceIntegrationTest {

    @Mock
    private NewsAiAnalysisRequesterPort newsAiAnalysisRequesterPort;

    private NewsAiProcessingService service;

    @BeforeEach
    void setUp() {
        service = new NewsAiProcessingService(newsAiAnalysisRequesterPort, command -> command.run());
    }

    @Test
    @DisplayName("내용이 있으면 분석된 뉴스를 반환한다")
    void processNewsWithAI_ValidContent_ReturnsProcessedNews() {
        // given
        Content content = new Content("Test Title", "Test content for AI analysis");
        NewsMeta newsMeta = NewsMeta.of(NewsProvider.BLOOMBERG,
                LocalDateTime.now(), "http://test.com");
        News news = News.createWithoutAI(newsMeta, content);
        when(newsAiAnalysisRequesterPort.analyseNewses(any(), any()))
                .thenReturn(List.of(news));

        // when
        News result = service.processNewsWithAI(news);

        // then
        assertNotNull(result);
        assertEquals(news.getId(), result.getId());
        verify(newsAiAnalysisRequesterPort, times(1)).analyseNewses(any(), any());
    }

    @Test
    @DisplayName("뉴스가 없으면 null을 반환한다")
    void processNewsWithAI_NullNews_ReturnsNull() {
        // given
        News news = null;

        // when
        News result = service.processNewsWithAI(news);

        // then
        assertNull(result);
        verify(newsAiAnalysisRequesterPort, never()).analyseNewses(any(), any());
    }

    @Test
    @DisplayName("내용이 비어 있으면 원본 뉴스를 반환한다")
    void processNewsWithAI_EmptyContent_ReturnsOriginalNews() {
        // given
        Content content = new Content("", "");
        NewsMeta newsMeta = NewsMeta.of(NewsProvider.BLOOMBERG,
                LocalDateTime.now(), "http://test.com");
        News news = News.createWithoutAI(newsMeta, content);
        when(newsAiAnalysisRequesterPort.analyseNewses(any(), any()))
                .thenReturn(List.of(news));

        // when
        News result = service.processNewsWithAI(news);

        // then
        assertNotNull(result);
        assertEquals(news.getId(), result.getId());
        verify(newsAiAnalysisRequesterPort, times(1)).analyseNewses(any(), any());
    }

    @Test
    @DisplayName("예외가 나면 원본 뉴스를 반환한다")
    void processNewsWithAI_ExceptionOccurs_ReturnsOriginalNews() {
        // given
        Content content = new Content("Test Title", "Test content");
        NewsMeta newsMeta = NewsMeta.of(NewsProvider.BLOOMBERG,
                LocalDateTime.now(), "http://test.com");
        News news = News.createWithoutAI(newsMeta, content);
        when(newsAiAnalysisRequesterPort.analyseNewses(any(), any()))
                .thenThrow(new RuntimeException("AI analysis failed"));

        // when
        News result = service.processNewsWithAI(news);

        // then
        assertNotNull(result);
        assertEquals(news.getId(), result.getId());
        verify(newsAiAnalysisRequesterPort, times(1)).analyseNewses(any(), any());
    }

    @Test
    @DisplayName("뉴스 목록이 있으면 분석된 목록을 반환한다")
    void processNewsList_ValidNewsList_ReturnsProcessedList() {
        // given
        Content content1 = new Content("Title 1", "Content 1");
        Content content2 = new Content("Title 2", "Content 2");
        NewsMeta newsMeta1 = NewsMeta.of(NewsProvider.BLOOMBERG,
                LocalDateTime.now(), "http://test1.com");
        NewsMeta newsMeta2 = NewsMeta.of(NewsProvider.MARKETAUX,
                LocalDateTime.now(), "http://test2.com");
        News news1 = News.createWithoutAI(newsMeta1, content1);
        News news2 = News.createWithoutAI(newsMeta2, content2);
        List<News> newsList = List.of(news1, news2);
        when(newsAiAnalysisRequesterPort.analyseNewses(any(), any()))
                .thenReturn(List.of(news1, news2));

        // when
        List<News> result = service.processNewsWithAI(newsList);

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(news1.getId(), result.get(0).getId());
        assertEquals(news2.getId(), result.get(1).getId());
        verify(newsAiAnalysisRequesterPort, times(2)).analyseNewses(any(), any());
    }

    @Test
    @DisplayName("빈 목록은 빈 목록을 반환한다")
    void processNewsList_EmptyList_ReturnsEmptyList() {
        // given
        List<News> newsList = List.of();

        // when
        List<News> result = service.processNewsWithAI(newsList);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(newsAiAnalysisRequesterPort, never()).analyseNewses(any(), any());
    }

    @Test
    @DisplayName("목록이 없으면 빈 목록을 반환한다")
    void processNewsList_NullList_ReturnsEmptyList() {
        // given

        // when
        List<News> result = service.processNewsWithAI((List<News>) null);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(newsAiAnalysisRequesterPort, never()).analyseNewses(any(), any());
    }

    @Test
    @DisplayName("특수문자가 있는 내용을 처리한다")
    void processNewsWithAI_ContentWithSpecialCharacters_HandlesCorrectly() {
        // given
        Content content = new Content("Test @user Title", "Content with http://example.com and special chars!");
        NewsMeta newsMeta = NewsMeta.of(NewsProvider.BLOOMBERG,
                LocalDateTime.now(), "http://test.com");
        News news = News.createWithoutAI(newsMeta, content);
        when(newsAiAnalysisRequesterPort.analyseNewses(any(), any()))
                .thenReturn(List.of(news));

        // when
        News result = service.processNewsWithAI(news);

        // then
        assertNotNull(result);
        assertEquals(news.getId(), result.getId());
        verify(newsAiAnalysisRequesterPort, times(1)).analyseNewses(any(), any());
    }

    @Test
    @DisplayName("긴 내용을 처리한다")
    void processNewsWithAI_LongContent_HandlesCorrectly() {
        // given
        String longContent = "A".repeat(1000);
        Content content = new Content("Long Title", longContent);
        NewsMeta newsMeta = NewsMeta.of(NewsProvider.BLOOMBERG,
                LocalDateTime.now(), "http://test.com");
        News news = News.createWithoutAI(newsMeta, content);
        when(newsAiAnalysisRequesterPort.analyseNewses(any(), any()))
                .thenReturn(List.of(news));

        // when
        News result = service.processNewsWithAI(news);

        // then
        assertNotNull(result);
        assertEquals(news.getId(), result.getId());
        verify(newsAiAnalysisRequesterPort, times(1)).analyseNewses(any(), any());
    }

    @Test
    @DisplayName("이미 분석이 있는 뉴스를 갱신한다")
    void processNewsWithAI_NewsWithExistingAiOverview_UpdatesCorrectly() {
        // given
        Content content = new Content("Test Title", "Test content");
        NewsMeta newsMeta = NewsMeta.of(NewsProvider.BLOOMBERG,
                LocalDateTime.now(), "http://test.com");
        News news = News.createWithoutAI(newsMeta, content);
        AiOverview existingOverview = new AiOverview(
                "Existing overview",
                SentimentType.NEUTRAL,
                0.5,
                List.of(TargetCategory.SPY));

        // when
        News newsWithAi = news.updateAiAnalysis(
                existingOverview.getOverview(),
                "Translated Title",
                "Translated content",
                existingOverview.getTargetCategories(),
                existingOverview.getSentimentType(),
                existingOverview.getSentimentScore());

        // given
        when(newsAiAnalysisRequesterPort.analyseNewses(any(), any()))
                .thenReturn(List.of(newsWithAi));

        // when
        News result = service.processNewsWithAI(newsWithAi);

        // then
        assertNotNull(result);
        assertEquals(newsWithAi.getId(), result.getId());
        verify(newsAiAnalysisRequesterPort, times(1)).analyseNewses(any(), any());
    }

    @Test
    @DisplayName("서비스를 쓸 수 없으면 원본을 유지한다")
    void processNewsWithAI_ServiceUnavailable_HandlesGracefully() {
        // given
        Content content = new Content("Test Title", "Test content");
        NewsMeta newsMeta = NewsMeta.of(NewsProvider.BLOOMBERG,
                LocalDateTime.now(), "http://test.com");
        News news = News.createWithoutAI(newsMeta, content);
        when(newsAiAnalysisRequesterPort.analyseNewses(any(), any()))
                .thenThrow(new RuntimeException("Service unavailable"));

        // when
        News result = service.processNewsWithAI(news);

        // then
        assertNotNull(result);
        assertEquals(news.getId(), result.getId());
        verify(newsAiAnalysisRequesterPort, times(1)).analyseNewses(any(), any());
    }

    @Test
    @DisplayName("시간 초과가 나도 원본을 유지한다")
    void processNewsWithAI_TimeoutScenario_HandlesCorrectly() {
        // given
        Content content = new Content("Test Title", "Test content");
        NewsMeta newsMeta = NewsMeta.of(NewsProvider.BLOOMBERG,
                LocalDateTime.now(), "http://test.com");
        News news = News.createWithoutAI(newsMeta, content);
        when(newsAiAnalysisRequesterPort.analyseNewses(any(), any()))
                .thenThrow(new RuntimeException("Request timeout"));

        // when
        News result = service.processNewsWithAI(news);

        // then
        assertNotNull(result);
        assertEquals(news.getId(), result.getId());
        verify(newsAiAnalysisRequesterPort, times(1)).analyseNewses(any(), any());
    }

    @Test
    @DisplayName("올바르지 않은 내용을 처리한다")
    void processNewsWithAI_InvalidContent_HandlesCorrectly() {
        // given
        Content content = new Content(null, null);
        NewsMeta newsMeta = NewsMeta.of(NewsProvider.BLOOMBERG,
                LocalDateTime.now(), "http://test.com");
        News news = News.createWithoutAI(newsMeta, content);
        when(newsAiAnalysisRequesterPort.analyseNewses(any(), any()))
                .thenReturn(List.of(news));

        // when
        News result = service.processNewsWithAI(news);

        // then
        assertNotNull(result);
        assertEquals(news.getId(), result.getId());
        verify(newsAiAnalysisRequesterPort, times(1)).analyseNewses(any(), any());
    }
}
