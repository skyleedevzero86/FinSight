package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.EconomyPickNewsView;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsQueryRequest;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsSearchRequest;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsSimilarSearchPort;
import com.sleekydz86.finsight.core.news.domain.vo.AiOverview;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.vo.NewsMeta;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EconomyPickNewsServiceTest {

    @Mock
    private UserPersistencePort userPersistencePort;

    @Test
    void 관심카테고리가_있으면_해당_뉴스만_최신순으로_반환한다() {
        News older = article(1L, "엔비디아 수주", TargetCategory.NVDA, "2026-10-01T09:00:00");
        News newer = article(2L, "엔비디아 실적", TargetCategory.NVDA, "2026-10-11T09:00:00");
        News other = article(3L, "애플 신제품", TargetCategory.AAPL, "2026-10-12T09:00:00");
        member("member@finsight.dev", List.of(TargetCategory.NVDA));
        EconomyPickNewsService service = service(List.of(older, newer, other), List.of());

        EconomyPickNewsView view = service.recommend("member@finsight.dev", 1);

        assertThat(view.mode()).isEqualTo("CATEGORY");
        assertThat(view.items()).extracting(EconomyPickNewsView.Item::id).containsExactly(2L);
        assertThat(view.items().get(0).source()).isEqualTo("마켓옥스");
        assertThat(view.items().get(0).href()).isEqualTo("https://example.com/2");
    }

    @Test
    void 관심카테고리가_NONE만이면_벡터추천으로_넘긴다() {
        member("member@finsight.dev", List.of(TargetCategory.NONE));
        News seed = article(10L, "코스닥 공시 점검", TargetCategory.GENERAL, "2026-10-10T09:00:00");
        News neighbor = article(11L, "관련 시그널", TargetCategory.GENERAL, "2026-10-10T10:00:00");
        EconomyPickNewsService service = service(List.of(seed, neighbor), List.of(11L));

        EconomyPickNewsView view = service.recommend("member@finsight.dev", 6);

        assertThat(view.mode()).isEqualTo("VECTOR");
        assertThat(view.items()).extracting(EconomyPickNewsView.Item::id).containsExactly(11L);
    }

    @Test
    void 비회원은_테마_씨앗의_유사뉴스를_추천한다() {
        News seed = article(10L, "코스닥 전환사채", TargetCategory.GENERAL, "2026-10-10T09:00:00");
        News neighbor = article(12L, "항공 수주", TargetCategory.GENERAL, "2026-10-10T11:00:00");
        EconomyPickNewsService service = service(List.of(seed, neighbor), List.of(12L));

        EconomyPickNewsView view = service.recommend(null, 6);

        assertThat(view.mode()).isEqualTo("VECTOR");
        assertThat(view.items()).extracting(EconomyPickNewsView.Item::title).containsExactly("항공 수주");
    }

    @Test
    void 벡터색인이_없으면_테마풀에서_추천한다() {
        News seed = article(10L, "코스닥 소송", TargetCategory.GENERAL, "2026-10-10T09:00:00");
        News extra = article(13L, "코스닥 대주주", TargetCategory.GENERAL, "2026-10-10T08:00:00");
        EconomyPickNewsService service = service(List.of(seed, extra), List.of());

        EconomyPickNewsView view = service.recommend(" ", 2);

        assertThat(view.mode()).isEqualTo("VECTOR");
        assertThat(view.items()).extracting(EconomyPickNewsView.Item::id).containsExactlyInAnyOrder(10L, 13L);
    }

    @Test
    void 더미뉴스와_빈결과는_추천에서_빠진다() {
        News dummy = article(9L, "[더미] 코스닥", TargetCategory.NVDA, "2026-10-12T09:00:00");
        member("member@finsight.dev", List.of(TargetCategory.NVDA));
        EconomyPickNewsService service = service(List.of(dummy), List.of());

        EconomyPickNewsView view = service.recommend("member@finsight.dev", 6);

        assertThat(view.mode()).isEqualTo("CATEGORY");
        assertThat(view.items()).isEmpty();
    }

    private void member(String email, List<TargetCategory> watchlist) {
        User user = User.builder().email(email).watchlist(watchlist).build();
        when(userPersistencePort.findByEmail(email)).thenReturn(Optional.of(user));
    }

    private EconomyPickNewsService service(List<News> rows, List<Long> similarIds) {
        return new EconomyPickNewsService(
                new CatalogNews(rows),
                userPersistencePort,
                new ReadySimilar(new FixedSimilar(similarIds)),
                bound -> 0);
    }

    private static News article(long id, String title, TargetCategory category, String published) {
        LocalDateTime time = LocalDateTime.parse(published);
        return new News(
                id,
                NewsProvider.MARKETAUX,
                time,
                new Content(title, "본문", null, null, null),
                new Content(title, "본문"),
                new AiOverview("요약", SentimentType.NEUTRAL, 0.1, List.of(category)),
                NewsMeta.of(NewsProvider.MARKETAUX, time, "https://example.com/" + id));
    }

    private static final class FixedSimilar implements NewsSimilarSearchPort {
        private final List<Long> ids;

        private FixedSimilar(List<Long> ids) {
            this.ids = ids;
        }

        @Override
        public List<Long> findSimilarIds(long newsId, int limit) {
            return ids;
        }
    }

    private static final class ReadySimilar implements ObjectProvider<NewsSimilarSearchPort> {
        private final NewsSimilarSearchPort port;

        private ReadySimilar(NewsSimilarSearchPort port) {
            this.port = port;
        }

        @Override
        public NewsSimilarSearchPort getObject() {
            return port;
        }

        @Override
        public NewsSimilarSearchPort getObject(Object... args) {
            return port;
        }

        @Override
        public NewsSimilarSearchPort getIfAvailable() {
            return port;
        }
    }

    private static final class CatalogNews implements NewsPersistencePort {
        private final List<News> rows;

        private CatalogNews(List<News> rows) {
            this.rows = rows;
        }

        @Override
        public Newses findAllByFilters(NewsQueryRequest request) {
            if (request.getCategories() != null && !request.getCategories().isEmpty()) {
                return new Newses(rows.stream().filter(news -> matches(news, request.getCategories())).toList());
            }
            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                throw new UnsupportedOperationException("본문 키워드 조회는 사용하지 않습니다");
            }
            return new Newses(rows);
        }

        @Override
        public Newses findByIds(List<Long> ids) {
            return new Newses(ids.stream().map(this::byId).filter(Objects::nonNull).toList());
        }

        @Override
        public Newses findLatestNews(int limit) {
            return new Newses(rows.stream().limit(limit).toList());
        }

        @Override
        public Newses saveAllNews(List<News> newses) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<News> omitStoredDuplicates(List<News> newses) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Newses findByOverviewIsNull() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<News> findById(Long newsId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Newses searchByQuery(NewsSearchRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Newses findByCategory(String category, int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Newses findPopularNews(int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Newses findLatestNewsByProvider(NewsProvider provider, int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Newses findRelatedNews(Long newsId, List<String> categories, int limit) {
            throw new UnsupportedOperationException();
        }

        private News byId(Long id) {
            return rows.stream().filter(news -> id.equals(news.getId())).findFirst().orElse(null);
        }

        private boolean matches(News news, List<TargetCategory> categories) {
            if (news.getAiOverView() == null || news.getAiOverView().getTargetCategories() == null) {
                return false;
            }
            return news.getAiOverView().getTargetCategories().stream().anyMatch(categories::contains);
        }
    }
}
