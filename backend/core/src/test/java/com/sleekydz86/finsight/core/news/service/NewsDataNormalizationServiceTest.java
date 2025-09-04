package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.vo.AiOverview;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.vo.ContentNormalizationResult;
import com.sleekydz86.finsight.core.news.domain.vo.NewsMeta;
import com.sleekydz86.finsight.core.news.domain.vo.NormalizedContent;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.global.NewsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
        // given
        List<News> newsList = List.of(testNews, htmlTestNews);

        // when
        List<News> normalizedNews = normalizationService.normalizeNewsList(newsList);

        // then
        assertThat(normalizedNews).isNotNull();
        assertThat(normalizedNews).hasSize(2);

        News firstNews = normalizedNews.get(0);
        assertThat(firstNews.getOriginalContent().getTitle()).isEqualTo("테스트 뉴스 제목");
        assertThat(firstNews.getOriginalContent().getBody()).isEqualTo("테스트 뉴스 본문입니다.");

        News secondNews = normalizedNews.get(1);
        assertThat(secondNews.getOriginalContent().getTitle()).isEqualTo("HTML 태그 포함 뉴스");
        assertThat(secondNews.getOriginalContent().getBody()).isEqualTo("HTML body with tags.");
    }

    @Test
    @DisplayName("HTML 태그 제거 정규화 성공")
    void HTML_태그_제거_정규화_성공() {
        // given
        News newsWithHtml = createNewsWithHtmlTags();

        // when
        List<News> normalizedNews = normalizationService.normalizeNewsList(List.of(newsWithHtml));

        // then
        assertThat(normalizedNews).hasSize(1);
        News normalized = normalizedNews.get(0);

        assertThat(normalized.getOriginalContent().getTitle())
                .doesNotContain("<", ">", "&lt;", "&gt;");
        assertThat(normalized.getOriginalContent().getBody())
                .doesNotContain("<", ">", "&lt;", "&gt;");
    }

    @Test
    @DisplayName("빈 데이터 처리 성공")
    void 빈_데이터_처리_성공() {
        // given
        List<News> newsList = List.of(emptyTestNews);

        // when
        List<News> normalizedNews = normalizationService.normalizeNewsList(newsList);

        // then
        assertThat(normalizedNews).hasSize(1);
        News normalized = normalizedNews.get(0);
        assertThat(normalized.getOriginalContent().getTitle()).isEqualTo("");
        assertThat(normalized.getOriginalContent().getBody()).isEqualTo("");
    }

    @Test
    @DisplayName("서비스 통계 조회 성공")
    void 서비스_통계_조회_성공() {
        // given
        List<News> newsList = List.of(testNews, htmlTestNews, emptyTestNews);

        // when
        normalizationService.normalizeNewsList(newsList);
        var statistics = normalizationService.getServiceStatistics();

        // then
        assertThat(statistics).isNotNull();
        assertThat(statistics.getTotalProcessed()).isEqualTo(3);
        assertThat(statistics.getSuccessCount()).isEqualTo(3);
        assertThat(statistics.getErrorCount()).isEqualTo(0);
        assertThat(statistics.getSuccessRate()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("특수문자 처리 정규화 성공")
    void 특수문자_처리_정규화_성공() {
        // given
        List<News> newsList = List.of(specialCharTestNews);

        // when
        List<News> normalizedNews = normalizationService.normalizeNewsList(newsList);

        // then
        assertThat(normalizedNews).hasSize(1);
        News normalized = normalizedNews.get(0);

        assertThat(normalized.getOriginalContent().getTitle())
                .contains("특수문자", "테스트", "제목");
        assertThat(normalized.getOriginalContent().getBody())
                .contains("특수문자", "테스트", "본문");
    }

    @Test
    @DisplayName("긴 컨텐츠 처리 정규화 성공")
    void 긴_컨텐츠_처리_정규화_성공() {
        // given
        List<News> newsList = List.of(longContentTestNews);

        // when
        List<News> normalizedNews = normalizationService.normalizeNewsList(newsList);

        // then
        assertThat(normalizedNews).hasSize(1);
        News normalized = normalizedNews.get(0);

        assertThat(normalized.getOriginalContent().getTitle()).isNotNull();
        assertThat(normalized.getOriginalContent().getBody()).isNotNull();
        assertThat(normalized.getOriginalContent().getBody().length()).isGreaterThan(1000);
    }

    @Test
    @DisplayName("중복 뉴스 제거 성공")
    void 중복_뉴스_제거_성공() {
        // given
        News duplicateNews = createDuplicateNews(testNews);
        List<News> newsList = List.of(testNews, duplicateNews);

        // when
        List<News> normalizedNews = normalizationService.normalizeNewsList(newsList);

        // then
        assertThat(normalizedNews).hasSize(1);
        assertThat(normalizedNews.get(0).getId()).isEqualTo(testNews.getId());
    }

    @Test
    @DisplayName("정규화 실패 시 예외 처리")
    void 정규화_실패_시_예외_처리() {
        // given
        News invalidNews = createInvalidNews();

        // when & then
        assertThatThrownBy(() -> normalizationService.normalizeNewsList(List.of(invalidNews)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("정규화 처리 중 오류가 발생했습니다");
    }

    @Test
    @DisplayName("대용량 뉴스 리스트 처리 성공")
    void 대용량_뉴스_리스트_처리_성공() {
        // given
        List<News> largeNewsList = createLargeNewsList(100);

        // when
        List<News> normalizedNews = normalizationService.normalizeNewsList(largeNewsList);

        // then
        assertThat(normalizedNews).hasSize(100);
        assertThat(normalizedNews).allMatch(news ->
                news.getOriginalContent() != null &&
                        news.getOriginalContent().getTitle() != null);
    }

    @Test
    @DisplayName("정규화 품질 검증 성공")
    void 정규화_품질_검증_성공() {
        // given
        List<News> newsList = List.of(testNews);

        // when
        List<News> normalizedNews = normalizationService.normalizeNewsList(newsList);
        var qualityReport = normalizationService.generateQualityReport(normalizedNews);

        // then
        assertThat(qualityReport).isNotNull();
        assertThat(qualityReport.getTotalNews()).isEqualTo(1);
        assertThat(qualityReport.getQualityScore()).isGreaterThan(80);
    }

    @Test
    @DisplayName("정규화 성능 측정 성공")
    void 정규화_성능_측정_성공() {
        // given
        List<News> newsList = createLargeNewsList(50);

        // when
        long startTime = System.currentTimeMillis();
        List<News> normalizedNews = normalizationService.normalizeNewsList(newsList);
        long endTime = System.currentTimeMillis();

        // then
        assertThat(normalizedNews).hasSize(50);
        long processingTime = endTime - startTime;
        assertThat(processingTime).isLessThan(5000);
    }

    @Test
    @DisplayName("정규화 설정 변경 성공")
    void 정규화_설정_변경_성공() {
        // given
        var originalConfig = normalizationService.getConfiguration();

        // when
        normalizationService.updateConfiguration(
                originalConfig.getMaxTitleLength() + 100,
                originalConfig.getMaxBodyLength() + 500,
                !originalConfig.isRemoveHtmlTags(),
                !originalConfig.isNormalizeUnicode()
        );
        var newConfig = normalizationService.getConfiguration();

        // then
        assertThat(newConfig.getMaxTitleLength()).isEqualTo(originalConfig.getMaxTitleLength() + 100);
        assertThat(newConfig.getMaxBodyLength()).isEqualTo(originalConfig.getMaxBodyLength() + 500);
        assertThat(newConfig.isRemoveHtmlTags()).isNotEqualTo(originalConfig.isRemoveHtmlTags());
        assertThat(newConfig.isNormalizeUnicode()).isNotEqualTo(originalConfig.isNormalizeUnicode());
    }

    @Test
    @DisplayName("정규화 히스토리 추적 성공")
    void 정규화_히스토리_추적_성공() {
        // given
        List<News> newsList = List.of(testNews);

        // when
        normalizationService.normalizeNewsList(newsList);
        var history = normalizationService.getProcessingHistory();

        // then
        assertThat(history).isNotNull();
        assertThat(history.getTotalProcessed()).isGreaterThan(0);
        assertThat(history.getLastProcessedTime()).isNotNull();
    }

    @Test
    @DisplayName("정규화 메트릭 수집 성공")
    void 정규화_메트릭_수집_성공() {
        // given
        List<News> newsList = List.of(testNews, htmlTestNews);

        // when
        normalizationService.normalizeNewsList(newsList);
        var metrics = normalizationService.collectMetrics();

        // then
        assertThat(metrics).isNotNull();
        assertThat(metrics.getProcessingTime()).isGreaterThan(0);
        assertThat(metrics.getMemoryUsage()).isGreaterThan(0);
        assertThat(metrics.getCpuUsage()).isGreaterThan(0);
    }

    @Test
    @DisplayName("유니코드 정규화 성공")
    void 유니코드_정규화_성공() {
        // given
        News unicodeNews = createUnicodeTestNews();

        // when
        List<News> normalizedNews = normalizationService.normalizeNewsList(List.of(unicodeNews));

        // then
        assertThat(normalizedNews).hasSize(1);
        News normalized = normalizedNews.get(0);

        assertThat(normalized.getOriginalContent().getTitle()).isNotNull();
        assertThat(normalized.getOriginalContent().getBody()).isNotNull();
    }

    @Test
    @DisplayName("이모지 처리 정규화 성공")
    void 이모지_처리_정규화_성공() {
        // given
        News emojiNews = createEmojiTestNews();

        // when
        List<News> normalizedNews = normalizationService.normalizeNewsList(List.of(emojiNews));

        // then
        assertThat(normalizedNews).hasSize(1);
        News normalized = normalizedNews.get(0);

        // 이모지가 적절히 처리되었는지 확인
        assertThat(normalized.getOriginalContent().getTitle()).isNotNull();
        assertThat(normalized.getOriginalContent().getBody()).isNotNull();
    }

    @Test
    @DisplayName("정규화 캐시 동작 확인")
    void 정규화_캐시_동작_확인() {
        // given
        List<News> newsList = List.of(testNews);

        // when
        normalizationService.normalizeNewsList(newsList);
        normalizationService.normalizeNewsList(newsList);

        // then
        var cacheStats = normalizationService.getCacheStatistics();
        assertThat(cacheStats).isNotNull();
        assertThat(cacheStats.getCacheHits()).isGreaterThan(0);
    }


    private News createTestNews() {
        return News.builder()
                .id(1L)
                .newsMeta(NewsMeta.of(
                        NewsProvider.BLOOMBERG,
                        OffsetDateTime.now(ZoneOffset.of("+09:00")),
                        "https://example.com/test"
                ))
                .scrapedTime(LocalDateTime.now())
                .originalContent(Content.builder()
                        .title("테스트 뉴스 제목")
                        .body("테스트 뉴스 본문입니다.")
                        .build())
                .translatedContent(Content.builder()
                        .title("Test News Title")
                        .body("This is the body of test news. It contains sufficiently long content.")
                        .build())
                .aiOverView(AiOverview.builder()
                        .overview("테스트 뉴스 요약")
                        .sentimentType(SentimentType.NEUTRAL)
                        .sentimentScore(0.0)
                        .categories(List.of(TargetCategory.BITCOIN))
                        .build())
                .targetCategories(List.of(TargetCategory.BITCOIN))
                .build();
    }

    private News createHtmlTestNews() {
        return News.builder()
                .id(2L)
                .newsMeta(NewsMeta.of(
                        NewsProvider.MARKETAUX,
                        OffsetDateTime.now(ZoneOffset.of("+09:00")),
                        "https://example.com/html-test"
                ))
                .scrapedTime(LocalDateTime.now())
                .originalContent(Content.builder()
                        .title("<h1>HTML 태그 포함 뉴스</h1>")
                        .body("<p>HTML body with tags.</p>")
                        .build())
                .translatedContent(Content.builder()
                        .title("HTML News with Tags")
                        .body("HTML body with tags.")
                        .build())
                .aiOverView(AiOverview.builder()
                        .overview("HTML 태그 포함 뉴스")
                        .sentimentType(SentimentType.NEUTRAL)
                        .sentimentScore(0.0)
                        .categories(List.of(TargetCategory.TESLA))
                        .build())
                .targetCategories(List.of(TargetCategory.TESLA))
                .build();
    }

    private News createEmptyTestNews() {
        return News.builder()
                .id(3L)
                .newsMeta(NewsMeta.of(
                        NewsProvider.BLOOMBERG,
                        OffsetDateTime.now(ZoneOffset.of("+09:00")),
                        "https://example.com/empty"
                ))
                .scrapedTime(LocalDateTime.now())
                .originalContent(Content.builder()
                        .title("")
                        .body("")
                        .build())
                .translatedContent(Content.builder()
                        .title("")
                        .body("")
                        .build())
                .aiOverView(AiOverview.builder()
                        .overview("빈 컨텐츠 뉴스")
                        .sentimentType(SentimentType.NEUTRAL)
                        .sentimentScore(0.0)
                        .categories(List.of())
                        .build())
                .targetCategories(List.of())
                .build();
    }

    private News createSpecialCharTestNews() {
        return News.builder()
                .id(4L)
                .newsMeta(NewsMeta.of(
                        NewsProvider.MARKETAUX,
                        OffsetDateTime.now(ZoneOffset.of("+09:00")),
                        "https://example.com/special"
                ))
                .scrapedTime(LocalDateTime.now())
                .originalContent(Content.builder()
                        .title("특수문자 테스트 제목!@#$%^&*()")
                        .body("특수문자 테스트 본문!@#$%^&*()_+-=[]{}|;':\",./<>?")
                        .build())
                .translatedContent(Content.builder()
                        .title("Special Character Test Title")
                        .body("Special Character Test Body")
                        .build())
                .aiOverView(AiOverview.builder()
                        .overview("특수문자 포함 뉴스")
                        .sentimentType(SentimentType.NEUTRAL)
                        .sentimentScore(0.0)
                        .categories(List.of(TargetCategory.BITCOIN))
                        .build())
                .targetCategories(List.of(TargetCategory.BITCOIN))
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
                        OffsetDateTime.now(ZoneOffset.of("+09:00")),
                        "https://example.com/long"
                ))
                .scrapedTime(LocalDateTime.now())
                .originalContent(Content.builder()
                        .title("매우 긴 제목을 가진 뉴스입니다. 이 제목은 정규화 서비스의 제목 길이 제한을 테스트하기 위해 충분히 길게 작성되었습니다.")
                        .body(longBody.toString())
                        .build())
                .translatedContent(Content.builder()
                        .title("Very Long News Title")
                        .body("Very long news body content for testing purposes.")
                        .build())
                .aiOverView(AiOverview.builder()
                        .overview("긴 컨텐츠를 가진 뉴스")
                        .sentimentType(SentimentType.NEUTRAL)
                        .sentimentScore(0.0)
                        .categories(List.of(TargetCategory.TESLA))
                        .build())
                .targetCategories(List.of(TargetCategory.TESLA))
                .build();
    }

    private News createNewsWithHtmlTags() {
        return News.builder()
                .id(6L)
                .newsMeta(NewsMeta.of(
                        NewsProvider.MARKETAUX,
                        OffsetDateTime.now(ZoneOffset.of("+09:00")),
                        "https://example.com/html"
                ))
                .scrapedTime(LocalDateTime.now())
                .originalContent(Content.builder()
                        .title("<h1>HTML 태그가 포함된 제목</h1><script>alert('xss')</script>")
                        .body("<p>HTML 태그가 포함된 본문입니다.</p><div>여러 줄의 <br>HTML 컨텐츠</div>")
                        .build())
                .translatedContent(Content.builder()
                        .title("HTML Title with Tags")
                        .body("HTML body with tags")
                        .build())
                .aiOverView(AiOverview.builder()
                        .overview("HTML 태그 포함 뉴스")
                        .sentimentType(SentimentType.NEUTRAL)
                        .sentimentScore(0.0)
                        .categories(List.of(TargetCategory.BITCOIN))
                        .build())
                .targetCategories(List.of(TargetCategory.BITCOIN))
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
                .targetCategories(originalNews.getTargetCategories())
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
                .targetCategories(null)
                .build();
    }

    private News createUnicodeTestNews() {
        return News.builder()
                .id(7L)
                .newsMeta(NewsMeta.of(
                        NewsProvider.BLOOMBERG,
                        OffsetDateTime.now(ZoneOffset.of("+09:00")),
                        "https://example.com/unicode"
                ))
                .scrapedTime(LocalDateTime.now())
                .originalContent(Content.builder()
                        .title("유니코드 테스트 제목: café résumé naïve")
                        .body("유니코드 테스트 본문: café résumé naïve, 中文, 日本語, 한국어")
                        .build())
                .translatedContent(Content.builder()
                        .title("Unicode Test Title")
                        .body("Unicode Test Body")
                        .build())
                .aiOverView(AiOverview.builder()
                        .overview("유니코드 포함 뉴스")
                        .sentimentType(SentimentType.NEUTRAL)
                        .sentimentScore(0.0)
                        .categories(List.of(TargetCategory.BITCOIN))
                        .build())
                .targetCategories(List.of(TargetCategory.BITCOIN))
                .build();
    }

    private News createEmojiTestNews() {
        return News.builder()
                .id(8L)
                .newsMeta(NewsMeta.of(
                        NewsProvider.MARKETAUX,
                        OffsetDateTime.now(ZoneOffset.of("+09:00")),
                        "https://example.com/emoji"
                ))
                .scrapedTime(LocalDateTime.now())
                .originalContent(Content.builder()
                        .title("이모지 테스트 제목 🚀📈💎")
                        .body("이모지 테스트 본문: 🚀  💎 Bitcoin is going to the moon! 🌙✨")
                        .build())
                .translatedContent(Content.builder()
                        .title("Emoji Test Title")
                        .body("Emoji Test Body")
                        .build())
                .aiOverView(AiOverview.builder()
                        .overview("이모지 포함 뉴스")
                        .sentimentType(SentimentType.POSITIVE)
                        .sentimentScore(0.8)
                        .categories(List.of(TargetCategory.BITCOIN))
                        .build())
                .targetCategories(List.of(TargetCategory.BITCOIN))
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
                        OffsetDateTime.now(ZoneOffset.of("+09:00")),
                        "https://example.com/news-" + id
                ))
                .scrapedTime(LocalDateTime.now())
                .originalContent(Content.builder()
                        .title("테스트 뉴스 제목 " + id)
                        .body("테스트 뉴스 본문 " + id + "입니다. 이 뉴스는 대용량 테스트를 위해 생성되었습니다.")
                        .build())
                .translatedContent(Content.builder()
                        .title("Test News Title " + id)
                        .body("Test News Body " + id + ". This news was created for large-scale testing.")
                        .build())
                .aiOverView(AiOverview.builder()
                        .overview("테스트 뉴스 " + id + " 요약")
                        .sentimentType(SentimentType.NEUTRAL)
                        .sentimentScore(0.0)
                        .categories(List.of(TargetCategory.BITCOIN))
                        .build())
                .targetCategories(List.of(TargetCategory.BITCOIN))
                .build();
    }
}