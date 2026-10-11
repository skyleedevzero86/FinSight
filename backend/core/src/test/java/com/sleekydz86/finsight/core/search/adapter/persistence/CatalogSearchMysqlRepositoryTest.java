package com.sleekydz86.finsight.core.search.adapter.persistence;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.media.youtube.adapter.persistence.command.YoutubeVideoMetaJpaEntity;
import com.sleekydz86.finsight.core.media.youtube.adapter.persistence.command.YoutubeVideoMetaJpaRepository;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeImportSourceType;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeImportStatus;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaEntity;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CatalogSearchMysqlRepositoryTest {

    @Autowired
    private NewsJpaRepository newsRepository;

    @Autowired
    private YoutubeVideoMetaJpaRepository videoRepository;

    @DynamicPropertySource
    static void mysql(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                "jdbc:mysql://127.0.0.1:3306/finsight_catalog_it?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8");
        registry.add("spring.datasource.username", () -> "finsight");
        registry.add("spring.datasource.password", () -> "root123");
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.profiles.active", () -> "catalog-it");
    }

    @Test
    void 대소문자와_원문_번역_본문을_MySQL에서_찾는다() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 10, 12, 0);
        newsRepository.save(news(now, "ETF 주간 전망", "원문에는 다른 이야기", null, null));
        newsRepository.save(news(now.minusHours(1), "시황", "alpha vantage wire", "번역 제목", "번역 본문에는 없음"));
        newsRepository.save(news(now.minusHours(2), "레시피", "재료", "유부초밥", "유부초밥과 식초"));
        newsRepository.save(news(now.minusHours(3), "[더미] ETF", "더미 본문 ETF", null, null));

        assertThat(titles("etf")).containsExactly("ETF 주간 전망");
        assertThat(titles("ETF")).containsExactly("ETF 주간 전망");
        assertThat(titles("alpha vantage")).containsExactly("번역 제목");
        assertThat(titles("유부초밥")).containsExactly("유부초밥");
        assertThat(titleOnly("etf")).containsExactly("ETF 주간 전망");
        assertThat(titleOnly("alpha vantage")).isEmpty();
    }

    @Test
    void 발행된_VOD_제목과_TEXT_요약을_찾는다() {
        videoRepository.save(video(11L, "abcdefghijk", "Weekly ETF Brief", "요약 없음", YoutubeImportStatus.PUBLISHED));
        videoRepository.save(video(12L, "aaaabbbbccc", "다른 영상", "유부초밥 만드는 법", YoutubeImportStatus.PUBLISHED));
        videoRepository.save(video(13L, "short", "ETF short", "요약", YoutubeImportStatus.PUBLISHED));
        videoRepository.save(video(14L, "zzzzzzzzzzz", "ETF hidden", "숨김", YoutubeImportStatus.HIDDEN));

        assertThat(videoTitles("etf")).containsExactly("Weekly ETF Brief");
        assertThat(videoTitles("유부초밥")).containsExactly("다른 영상");
    }

    private List<String> titleOnly(String keyword) {
        return newsRepository.searchCatalogTitle(keyword, PageRequest.of(0, 10)).getContent().stream()
                .map(row -> row.getTranslatedTitle() != null && !row.getTranslatedTitle().isBlank()
                        ? row.getTranslatedTitle()
                        : row.getOriginalTitle())
                .toList();
    }

    private List<String> titles(String keyword) {
        return newsRepository.searchCatalog(keyword, PageRequest.of(0, 10)).getContent().stream()
                .map(row -> row.getTranslatedTitle() != null && !row.getTranslatedTitle().isBlank()
                        ? row.getTranslatedTitle()
                        : row.getOriginalTitle())
                .toList();
    }

    private List<String> videoTitles(String keyword) {
        return videoRepository.searchPublishedCatalog(keyword, PageRequest.of(0, 10)).getContent().stream()
                .map(YoutubeVideoMetaJpaEntity::getYoutubeTitle)
                .toList();
    }

    private NewsJpaEntity news(LocalDateTime at, String originalTitle, String originalContent,
                               String translatedTitle, String translatedContent) {
        return new NewsJpaEntity(null, NewsProvider.YAHOO_FINANCE, at, "https://example.test/" + originalTitle,
                at, originalTitle, originalContent, translatedTitle, translatedContent, null, null, null, 0, List.of());
    }

    private YoutubeVideoMetaJpaEntity video(long boardId, String videoId, String title, String summary,
                                            YoutubeImportStatus status) {
        YoutubeVideoMetaJpaEntity row = new YoutubeVideoMetaJpaEntity();
        row.setBoardId(boardId);
        row.setVideoId(videoId);
        row.setSourceType(YoutubeImportSourceType.MANUAL_URL);
        row.setYoutubeTitle(title);
        row.setSummary(summary);
        row.setImportStatus(status);
        return row;
    }
}
