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
        service = new NewsAiProcessingService(newsAiAnalysisRequesterPort);
    }

    @Test
    void processNewsWithAI_ValidContent_ReturnsProcessedNews() {
        Content content = new Content("Test Title", "Test content for AI analysis");
        NewsMeta newsMeta = NewsMeta.of(NewsProvider.BLOOMBERG,
                LocalDateTime.now(), "http://test.com");
        News news = News.createWithoutAI(newsMeta, content);

        when(newsAiAnalysisRequesterPort.analyseNewses(any(), any()))
                .thenReturn(List.of(news));

        News result = service.processNewsWithAI(news);

        assertNotNull(result);
        assertEquals(news.getId(), result.getId());
        verify(newsAiAnalysisRequesterPort, times(1)).analyseNewses(any(), any());
    }

    @Test
    void processNewsWithAI_NullNews_ReturnsNull() {
        News news = null;

        News result = service.processNewsWithAI(news);

        assertNull(result);
        verify(newsAiAnalysisRequesterPort, never()).analyseNewses(any(), any());
    }

    @Test
    void processNewsWithAI_EmptyContent_ReturnsOriginalNews() {
        Content content = new Content("", "");
        NewsMeta newsMeta = NewsMeta.of(NewsProvider.BLOOMBERG,
                LocalDateTime.now(), "http://test.com");
        News news = News.createWithoutAI(newsMeta, content);

        when(newsAiAnalysisRequesterPort.analyseNewses(any(), any()))
                .thenReturn(List.of(news));

        News result = service.processNewsWithAI(news);

        assertNotNull(result);
        assertEquals(news.getId(), result.getId());

        verify(newsAiAnalysisRequesterPort, times(1)).analyseNewses(any(), any());
    }

    @Test
    void processNewsWithAI_ExceptionOccurs_ReturnsOriginalNews() {
        Content content = new Content("Test Title", "Test content");
        NewsMeta newsMeta = NewsMeta.of(NewsProvider.BLOOMBERG,
                LocalDateTime.now(), "http://test.com");
        News news = News.createWithoutAI(newsMeta, content);

        when(newsAiAnalysisRequesterPort.analyseNewses(any(), any()))
                .thenThrow(new RuntimeException("AI analysis failed"));

        News result = service.processNewsWithAI(news);

        assertNotNull(result);
        assertEquals(news.getId(), result.getId());

        verify(newsAiAnalysisRequesterPort, times(1)).analyseNewses(any(), any());
    }

    @Test
    void processNewsList_ValidNewsList_ReturnsProcessedList() {
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

        List<News> result = service.processNewsWithAI(newsList);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(news1.getId(), result.get(0).getId());
        assertEquals(news2.getId(), result.get(1).getId());
        verify(newsAiAnalysisRequesterPort, times(2)).analyseNewses(any(), any());
    }

    @Test
    void processNewsList_EmptyList_ReturnsEmptyList() {
        List<News> newsList = List.of();

        List<News> result = service.processNewsWithAI(newsList);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(newsAiAnalysisRequesterPort, never()).analyseNewses(any(), any());
    }

    @Test
    void processNewsList_NullList_ThrowsNullPointerException() {
        List<News> newsList = null;

        assertThrows(NullPointerException.class, () -> {
            service.processNewsWithAI(newsList);
        });

        verify(newsAiAnalysisRequesterPort, never()).analyseNewses(any(), any());
    }

    @Test
    void processNewsWithAI_ContentWithSpecialCharacters_HandlesCorrectly() {
        Content content = new Content("Test @user Title", "Content with http://example.com and special chars!");
        NewsMeta newsMeta = NewsMeta.of(NewsProvider.BLOOMBERG,
                LocalDateTime.now(), "http://test.com");
        News news = News.createWithoutAI(newsMeta, content);

        when(newsAiAnalysisRequesterPort.analyseNewses(any(), any()))
                .thenReturn(List.of(news));

        News result = service.processNewsWithAI(news);

        assertNotNull(result);
        assertEquals(news.getId(), result.getId());
        verify(newsAiAnalysisRequesterPort, times(1)).analyseNewses(any(), any());
    }

    @Test
    void processNewsWithAI_LongContent_HandlesCorrectly() {
        String longContent = "A".repeat(1000);
        Content content = new Content("Long Title", longContent);
        NewsMeta newsMeta = NewsMeta.of(NewsProvider.BLOOMBERG,
                LocalDateTime.now(), "http://test.com");
        News news = News.createWithoutAI(newsMeta, content);

        when(newsAiAnalysisRequesterPort.analyseNewses(any(), any()))
                .thenReturn(List.of(news));

        News result = service.processNewsWithAI(news);

        assertNotNull(result);
        assertEquals(news.getId(), result.getId());
        verify(newsAiAnalysisRequesterPort, times(1)).analyseNewses(any(), any());
    }

    @Test
    void processNewsWithAI_NewsWithExistingAiOverview_UpdatesCorrectly() {
        Content content = new Content("Test Title", "Test content");
        NewsMeta newsMeta = NewsMeta.of(NewsProvider.BLOOMBERG,
                LocalDateTime.now(), "http://test.com");
        News news = News.createWithoutAI(newsMeta, content);

        AiOverview existingOverview = new AiOverview(
                "Existing overview",
                SentimentType.NEUTRAL,
                0.5,
                List.of(TargetCategory.SPY));

        News newsWithAi = news.updateAiAnalysis(
                existingOverview.getOverview(),
                "Translated Title",
                "Translated content",
                existingOverview.getTargetCategories(),
                existingOverview.getSentimentType(),
                existingOverview.getSentimentScore());

        when(newsAiAnalysisRequesterPort.analyseNewses(any(), any()))
                .thenReturn(List.of(newsWithAi));

        News result = service.processNewsWithAI(newsWithAi);

        assertNotNull(result);
        assertEquals(newsWithAi.getId(), result.getId());
        verify(newsAiAnalysisRequesterPort, times(1)).analyseNewses(any(), any());
    }

    @Test
    void processNewsWithAI_ServiceUnavailable_HandlesGracefully() {
        Content content = new Content("Test Title", "Test content");
        NewsMeta newsMeta = NewsMeta.of(NewsProvider.BLOOMBERG,
                LocalDateTime.now(), "http://test.com");
        News news = News.createWithoutAI(newsMeta, content);

        when(newsAiAnalysisRequesterPort.analyseNewses(any(), any()))
                .thenThrow(new RuntimeException("Service unavailable"));

        News result = service.processNewsWithAI(news);

        assertNotNull(result);
        assertEquals(news.getId(), result.getId());
        verify(newsAiAnalysisRequesterPort, times(1)).analyseNewses(any(), any());
    }

    @Test
    void processNewsWithAI_TimeoutScenario_HandlesCorrectly() {
        Content content = new Content("Test Title", "Test content");
        NewsMeta newsMeta = NewsMeta.of(NewsProvider.BLOOMBERG,
                LocalDateTime.now(), "http://test.com");
        News news = News.createWithoutAI(newsMeta, content);

        when(newsAiAnalysisRequesterPort.analyseNewses(any(), any()))
                .thenThrow(new RuntimeException("Request timeout"));

        News result = service.processNewsWithAI(news);

        assertNotNull(result);
        assertEquals(news.getId(), result.getId());
        verify(newsAiAnalysisRequesterPort, times(1)).analyseNewses(any(), any());
    }

    @Test
    void processNewsWithAI_InvalidContent_HandlesCorrectly() {
        Content content = new Content(null, null);
        NewsMeta newsMeta = NewsMeta.of(NewsProvider.BLOOMBERG,
                LocalDateTime.now(), "http://test.com");
        News news = News.createWithoutAI(newsMeta, content);

        when(newsAiAnalysisRequesterPort.analyseNewses(any(), any()))
                .thenReturn(List.of(news));

        News result = service.processNewsWithAI(news);

        assertNotNull(result);
        assertEquals(news.getId(), result.getId());

        verify(newsAiAnalysisRequesterPort, times(1)).analyseNewses(any(), any());
    }
}