package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.vo.AiOverview;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.vo.NewsMeta;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.global.NewsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("뉴스 데이터 정규화 서비스 테스트")
class NewsDataNormalizationServiceTest {

    @InjectMocks
    private NewsDataNormalizationService normalizationService;

    private News testNews;
    private News htmlTestNews;
    private News emptyTestNews;
    private News specialCharTestNews;
    private News longContentTestNews;

    @BeforeEach
    void setUp() {
        testNews = createTestNews();
        htmlTestNews = createHtmlTestNews();
        emptyTestNews = createEmptyTestNews();
        specialCharTestNews = createSpecialCharTestNews();
        longContentTestNews = createLongContentTestNews();
    }

    @Test
    @DisplayName("뉴스 데이터 정규화 및 중복제거 성공")
    void 뉴스_데이터_정규화_및_중복제거_성공() {
        
        List<News> newsList = List.of(testNews, htmlTestNews);

        
        List<News> normalizedNews = normalizationService.normalizeAndDeduplicate(newsList);

        
        assertThat(normalizedNews).isNotNull();
        assertThat(normalizedNews).hasSize(2);

        News firstNews = normalizedNews.get(0);
        assertThat(firstNews.getOriginalContent().getTitle()).isEqualTo("테스트 뉴스 제목");
        assertThat(firstNews.getOriginalContent().getContent()).isEqualTo("테스트 뉴스 본문입니다.");

        News secondNews = normalizedNews.get(1);
        assertThat(secondNews.getOriginalContent().getTitle()).isEqualTo("HTML 태그 포함 뉴스");
        assertThat(secondNews.getOriginalContent().getContent()).isEqualTo("HTML body with tags.");
    }

    @Test
    @DisplayName("HTML 태그 제거 정규화 성공")
    void HTML_태그_제거_정규화_성공() {
        
        News newsWithHtml = createNewsWithHtmlTags();

        
        List<News> normalizedNews = normalizationService.normalizeAndDeduplicate(List.of(newsWithHtml));

        
        assertThat(normalizedNews).hasSize(1);
        News normalized = normalizedNews.get(0);

        assertThat(normalized.getOriginalContent().getTitle())
                .doesNotContain("<", ">", "&lt;", "&gt;");
        assertThat(normalized.getOriginalContent().getContent())
                .doesNotContain("<", ">", "&lt;", "&gt;");
    }

    @Test
    @DisplayName("빈 데이터 처리 성공")
    void 빈_데이터_처리_성공() {
        
        List<News> newsList = List.of(emptyTestNews);

        
        List<News> normalizedNews = normalizationService.normalizeAndDeduplicate(newsList);

        
        assertThat(normalizedNews).hasSize(1);
        News normalized = normalizedNews.get(0);
        assertThat(normalized.getOriginalContent().getTitle()).isEqualTo("");
        assertThat(normalized.getOriginalContent().getContent()).isEqualTo("");
    }

    @Test
    @DisplayName("서비스 통계 조회 성공")
    void 서비스_통계_조회_성공() {
        
        List<News> newsList = List.of(testNews, htmlTestNews, emptyTestNews);

        
        normalizationService.normalizeAndDeduplicate(newsList);
        Map<String, Object> statistics = normalizationService.getServiceStatistics();

        
        assertThat(statistics).isNotNull();
        assertThat(statistics.get("totalProcessedCount")).isEqualTo(3L);
        assertThat(statistics.get("successCount")).isEqualTo(3L);
        assertThat(statistics.get("failureCount")).isEqualTo(0L);
        assertThat((Double) statistics.get("successRate")).isEqualTo(100.0);
    }

    @Test
    @DisplayName("특수문자 처리 정규화 성공")
    void 특수문자_처리_정규화_성공() {
        
        List<News> newsList = List.of(specialCharTestNews);

        
        List<News> normalizedNews = normalizationService.normalizeAndDeduplicate(newsList);

        
        assertThat(normalizedNews).hasSize(1);
        News normalized = normalizedNews.get(0);

        assertThat(normalized.getOriginalContent().getTitle())
                .contains("특수문자", "테스트", "제목");
        assertThat(normalized.getOriginalContent().getContent())
                .contains("특수문자", "테스트", "본문");
    }

    @Test
    @DisplayName("긴 컨텐츠 처리 정규화 성공")
    void 긴_컨텐츠_처리_정규화_성공() {
        
        List<News> newsList = List.of(longContentTestNews);

        
        List<News> normalizedNews = normalizationService.normalizeAndDeduplicate(newsList);

        
        assertThat(normalizedNews).hasSize(1);
        News normalized = normalizedNews.get(0);

        assertThat(normalized.getOriginalContent().getTitle()).isNotNull();
        assertThat(normalized.getOriginalContent().getContent()).isNotNull();
        assertThat(normalized.getOriginalContent().getContent().length()).isGreaterThan(1000);
    }

    @Test
    @DisplayName("중복 뉴스 제거 성공")
    void 중복_뉴스_제거_성공() {
        
        News duplicateNews = createDuplicateNews(testNews);
        List<News> newsList = List.of(testNews, duplicateNews);

        
        List<News> normalizedNews = normalizationService.normalizeAndDeduplicate(newsList);

        
        assertThat(normalizedNews).hasSize(1);
        assertThat(normalizedNews.get(0).getId()).isEqualTo(testNews.getId());
    }

    @Test
    @DisplayName("정규화 실패 시 예외 처리")
    void 정규화_실패_시_예외_처리() {
        
        News invalidNews = createInvalidNews();

        
        assertThatThrownBy(() -> normalizationService.normalizeAndDeduplicate(List.of(invalidNews)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("뉴스 정규화 중 오류 발생");
    }

    @Test
    @DisplayName("대용량 뉴스 리스트 처리 성공")
    void 대용량_뉴스_리스트_처리_성공() {
        
        List<News> largeNewsList = createLargeNewsList(100);

        
        List<News> normalizedNews = normalizationService.normalizeAndDeduplicate(largeNewsList);

        
        assertThat(normalizedNews).hasSize(100);
        assertThat(normalizedNews).allMatch(news ->
                news.getOriginalContent() != null &&
                        news.getOriginalContent().getTitle() != null);
    }

    @Test
    @DisplayName("정규화 성능 측정 성공")
    void 정규화_성능_측정_성공() {
        
        List<News> newsList = createLargeNewsList(50);

        
        long startTime = System.currentTimeMillis();
        List<News> normalizedNews = normalizationService.normalizeAndDeduplicate(newsList);
        long endTime = System.currentTimeMillis();

        
        assertThat(normalizedNews).hasSize(50);
        long processingTime = endTime - startTime;
        assertThat(processingTime).isLessThan(5000);
    }

    @Test
    @DisplayName("유니코드 정규화 성공")
    void 유니코드_정규화_성공() {
        
        News unicodeNews = createUnicodeTestNews();

        
        List<News> normalizedNews = normalizationService.normalizeAndDeduplicate(List.of(unicodeNews));

        
        assertThat(normalizedNews).hasSize(1);
        News normalized = normalizedNews.get(0);

        assertThat(normalized.getOriginalContent().getTitle()).isNotNull();
        assertThat(normalized.getOriginalContent().getContent()).isNotNull();
    }

    @Test
    @DisplayName("이모지 처리 정규화 성공")
    void 이모지_처리_정규화_성공() {
        
        News emojiNews = createEmojiTestNews();

        
        List<News> normalizedNews = normalizationService.normalizeAndDeduplicate(List.of(emojiNews));

        
        assertThat(normalizedNews).hasSize(1);
        News normalized = normalizedNews.get(0);

        assertThat(normalized.getOriginalContent().getTitle()).isNotNull();
        assertThat(normalized.getOriginalContent().getContent()).isNotNull();
    }

    @Test
    @DisplayName("캐시 클리어 성공")
    void 캐시_클리어_성공() {
        
        List<News> newsList = List.of(testNews);

        
        normalizationService.normalizeAndDeduplicate(newsList);
        normalizationService.clearCache();

        
        Map<String, Object> statistics = normalizationService.getServiceStatistics();
        assertThat(statistics.get("cacheSize")).isEqualTo(0);
    }

    private News createTestNews() {
        return News.builder()
                .id(1L)
                .newsMeta(NewsMeta.of(
                        NewsProvider.BLOOMBERG,
                        LocalDateTime.now(),
                        "https://example.com/test"
                ))
                .scrapedTime(LocalDateTime.now())
                .originalContent(new Content("테스트 뉴스 제목", "테스트 뉴스 본문입니다."))
                .translatedContent(new Content("Test News Title", "This is the body of test news. It contains sufficiently long content."))
                .aiOverView(new AiOverview("테스트 뉴스 요약", SentimentType.NEUTRAL, 0.0, List.of(TargetCategory.BITCOIN)))
                .build();
    }

    private News createHtmlTestNews() {
        return News.builder()
                .id(2L)
                .newsMeta(NewsMeta.of(
                        NewsProvider.MARKETAUX,
                        LocalDateTime.now(),
                        "https://example.com/html-test"
                ))
                .scrapedTime(LocalDateTime.now())
                .originalContent(new Content("<h1>HTML 태그 포함 뉴스</h1>", "<p>HTML body with tags.</p>"))
                .translatedContent(new Content("HTML News with Tags", "HTML body with tags."))
                .aiOverView(new AiOverview("HTML 태그 포함 뉴스", SentimentType.NEUTRAL, 0.0, List.of(TargetCategory.TSLA)))
                .build();
    }

    private News createEmptyTestNews() {
        return News.builder()
                .id(3L)
                .newsMeta(NewsMeta.of(
                        NewsProvider.BLOOMBERG,
                        LocalDateTime.now(),
                        "https://example.com/empty"
                ))
                .scrapedTime(LocalDateTime.now())
                .originalContent(new Content("", ""))
                .translatedContent(new Content("", ""))
                .aiOverView(new AiOverview("빈 컨텐츠 뉴스", SentimentType.NEUTRAL, 0.0, List.of()))
                .build();
    }

    private News createSpecialCharTestNews() {
        return News.builder()
                .id(4L)
                .newsMeta(NewsMeta.of(
                        NewsProvider.MARKETAUX,
                        LocalDateTime.now(),
                        "https://example.com/special"
                ))
                .scrapedTime(LocalDateTime.now())
                .originalContent(new Content("특수문자 테스트 제목!@#$%^&*()", "특수문자 테스트 본문!@#$%^&*()_+-=[]{}|;':\",./<>?"))
                .translatedContent(new Content("Special Character Test Title", "Special Character Test Body"))
                .aiOverView(new AiOverview("특수문자 포함 뉴스", SentimentType.NEUTRAL, 0.0, List.of(TargetCategory.BITCOIN)))
                .build();
    }

    private News createLongContentTestNews() {
        StringBuilder longBody = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longBody.append("이것은 매우 긴 뉴스 본문입니다. ")
                    .append("각 문장은 충분한 길이를 가지고 있으며, ")
                    .append("전체적으로 1000자 이상의 컨텐츠를 구성합니다. ")
                    .append("이러한 긴 컨텐츠는 정규화 서비스의 성능을 테스트하기에 적합합니다. ");
        }

        return News.builder()
                .id(5L)
                .newsMeta(NewsMeta.of(
                        NewsProvider.BLOOMBERG,
                        LocalDateTime.now(),
                        "https://example.com/long"
                ))
                .scrapedTime(LocalDateTime.now())
                .originalContent(new Content("매우 긴 제목을 가진 뉴스입니다. 이 제목은 정규화 서비스의 제목 길이 제한을 테스트하기 위해 충분히 길게 작성되었습니다.", longBody.toString()))
                .translatedContent(new Content("Very Long News Title", "Very long news body content for testing purposes."))
                .aiOverView(new AiOverview("긴 컨텐츠를 가진 뉴스", SentimentType.NEUTRAL, 0.0, List.of(TargetCategory.TSLA)))
                .build();
    }

    private News createNewsWithHtmlTags() {
        return News.builder()
                .id(6L)
                .newsMeta(NewsMeta.of(
                        NewsProvider.MARKETAUX,
                        LocalDateTime.now(),
                        "https://example.com/html"
                ))
                .scrapedTime(LocalDateTime.now())
                .originalContent(new Content("<h1>HTML 태그가 포함된 제목</h1><script>alert('xss')</script>", "<p>HTML 태그가 포함된 본문입니다.</p><div>여러 줄의 <br>HTML 컨텐츠</div>"))
                .translatedContent(new Content("HTML Title with Tags", "HTML body with tags"))
                .aiOverView(new AiOverview("HTML 태그 포함 뉴스", SentimentType.NEUTRAL, 0.0, List.of(TargetCategory.BITCOIN)))
                .build();
    }

    private News createDuplicateNews(News originalNews) {
        return News.builder()
                .id(originalNews.getId() + 1000)
                .newsMeta(originalNews.getNewsMeta())
                .scrapedTime(originalNews.getScrapedTime())
                .originalContent(originalNews.getOriginalContent())
                .translatedContent(originalNews.getTranslatedContent())
                .aiOverView(originalNews.getAiOverView())
                .build();
    }

    private News createInvalidNews() {
        return News.builder()
                .id(999L)
                .newsMeta(null)
                .scrapedTime(LocalDateTime.now())
                .originalContent(null)
                .translatedContent(null)
                .aiOverView(null)
                .build();
    }

    private News createUnicodeTestNews() {
        return News.builder()
                .id(7L)
                .newsMeta(NewsMeta.of(
                        NewsProvider.BLOOMBERG,
                        LocalDateTime.now(),
                        "https://example.com/unicode"
                ))
                .scrapedTime(LocalDateTime.now())
                .originalContent(new Content("유니코드 테스트 제목: café résumé naïve", "유니코드 테스트 본문: café résumé naïve, 中文, 日本語, 한국어"))
                .translatedContent(new Content("Unicode Test Title", "Unicode Test Body"))
                .aiOverView(new AiOverview("유니코드 포함 뉴스", SentimentType.NEUTRAL, 0.0, List.of(TargetCategory.BITCOIN)))
                .build();
    }

    private News createEmojiTestNews() {
        return News.builder()
                .id(8L)
                .newsMeta(NewsMeta.of(
                        NewsProvider.MARKETAUX,
                        LocalDateTime.now(),
                        "https://example.com/emoji"
                ))
                .scrapedTime(LocalDateTime.now())
                .originalContent(new Content("이모지 테스트 제목 ��📈��", "이모지 테스트 본문: 🚀  💎 Bitcoin is going to the moon! 🌙✨"))
                .translatedContent(new Content("Emoji Test Title", "Emoji Test Body"))
                .aiOverView(new AiOverview("이모지 포함 뉴스", SentimentType.POSITIVE, 0.8, List.of(TargetCategory.BITCOIN)))
                .build();
    }

    private List<News> createLargeNewsList(int size) {
        List<News> newsList = new java.util.ArrayList<>();
        for (int i = 0; i < size; i++) {
            newsList.add(createTestNewsWithId((long) i + 1000));
        }
        return newsList;
    }

    private News createTestNewsWithId(Long id) {
        return News.builder()
                .id(id)
                .newsMeta(NewsMeta.of(
                        NewsProvider.BLOOMBERG,
                        LocalDateTime.now(),
                        "https://example.com/news-" + id
                ))
                .scrapedTime(LocalDateTime.now())
                .originalContent(new Content("테스트 뉴스 제목 " + id, "테스트 뉴스 본문 " + id + "입니다. 이 뉴스는 대용량 테스트를 위해 생성되었습니다."))
                .translatedContent(new Content("Test News Title " + id, "Test News Body " + id + ". This news was created for large-scale testing."))
                .aiOverView(new AiOverview("테스트 뉴스 " + id + " 요약", SentimentType.NEUTRAL, 0.0, List.of(TargetCategory.BITCOIN)))
                .build();
    }
}