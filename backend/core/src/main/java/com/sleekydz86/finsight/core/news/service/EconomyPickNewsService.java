package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.EconomyPickNewsView;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsQueryRequest;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsSimilarSearchPort;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.vo.NewsMeta;
import com.sleekydz86.finsight.core.news.domain.vo.NewsTextRepair;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntUnaryOperator;

@Service
@Transactional(readOnly = true)
public class EconomyPickNewsService {

    private static final Logger log = LoggerFactory.getLogger(EconomyPickNewsService.class);
    private static final int POOL = 40;

    private final NewsPersistencePort newsPersistencePort;
    private final UserPersistencePort userPersistencePort;
    private final ObjectProvider<NewsSimilarSearchPort> similarSearch;
    private final IntUnaryOperator draw;

    @Autowired
    public EconomyPickNewsService(NewsPersistencePort newsPersistencePort,
                                  UserPersistencePort userPersistencePort,
                                  ObjectProvider<NewsSimilarSearchPort> similarSearch) {
        this(newsPersistencePort, userPersistencePort, similarSearch,
                bound -> ThreadLocalRandom.current().nextInt(bound));
    }

    EconomyPickNewsService(NewsPersistencePort newsPersistencePort,
                           UserPersistencePort userPersistencePort,
                           ObjectProvider<NewsSimilarSearchPort> similarSearch,
                           IntUnaryOperator draw) {
        this.newsPersistencePort = newsPersistencePort;
        this.userPersistencePort = userPersistencePort;
        this.similarSearch = similarSearch;
        this.draw = draw;
    }

    public EconomyPickNewsView recommend(String email, int limit) {
        int size = clamp(limit);
        List<TargetCategory> categories = watchlistOf(email);
        if (!categories.isEmpty()) {
            return view("CATEGORY", newest(matched(categories), size));
        }
        return view("VECTOR", vectorPick(size));
    }

    private List<TargetCategory> watchlistOf(String email) {
        if (email == null || email.isBlank()) {
            return List.of();
        }
        return userPersistencePort.findByEmail(email)
                .map(User::getWatchlist)
                .orElse(List.of())
                .stream()
                .filter(category -> category != null && category != TargetCategory.NONE)
                .distinct()
                .toList();
    }

    private List<News> matched(List<TargetCategory> categories) {
        NewsQueryRequest request = new NewsQueryRequest(null, null, null, null, categories, null);
        return real(newsPersistencePort.findAllByFilters(request).getNewses());
    }

    private List<News> vectorPick(int size) {
        EconomyPickLens lens = lens();
        List<News> pool = poolOf(lens);
        if (pool.isEmpty()) {
            log.info("벡터 추천에 사용할 뉴스가 없습니다. lens={}", lens);
            return List.of();
        }
        News seed = pool.get(draw.applyAsInt(pool.size()));
        List<News> similar = loadSimilar(seed.getId(), size);
        if (!similar.isEmpty()) {
            log.info("벡터 종목뉴스 추천 lens={} seedId={} count={}", lens, seed.getId(), similar.size());
            return similar;
        }
        log.info("벡터 색인이 없어 테마 풀에서 추천합니다. lens={} seedId={}", lens, seed.getId());
        return shuffled(pool, size);
    }

    private List<News> poolOf(EconomyPickLens lens) {
        String keyword = keyword(lens);
        List<News> latest = real(newsPersistencePort.findLatestNews(POOL).getNewses());
        List<News> matched = latest.stream().filter(news -> mentions(news, keyword)).toList();
        if (!matched.isEmpty()) {
            return cap(matched);
        }
        log.info("테마 키워드에 맞는 뉴스가 없어 최신 뉴스로 벡터 추천합니다. lens={} keyword={}", lens, keyword);
        return latest;
    }

    private boolean mentions(News news, String keyword) {
        return holds(text(news.getTranslatedContent()), keyword)
                || holds(body(news.getTranslatedContent()), keyword)
                || holds(text(news.getOriginalContent()), keyword)
                || holds(body(news.getOriginalContent()), keyword);
    }

    private boolean holds(String value, String keyword) {
        return value != null && keyword != null && value.contains(keyword);
    }

    private String body(Content content) {
        return content == null ? null : content.getContent();
    }

    private List<News> loadSimilar(Long newsId, int size) {
        if (newsId == null) {
            return List.of();
        }
        List<Long> ids = similarIds(newsId, size);
        if (ids.isEmpty()) {
            return List.of();
        }
        return ordered(newsPersistencePort.findByIds(ids).getNewses(), ids);
    }

    private List<Long> similarIds(long newsId, int limit) {
        NewsSimilarSearchPort search = similarSearch.getIfAvailable();
        if (search == null) {
            return List.of();
        }
        try {
            List<Long> ids = search.findSimilarIds(newsId, limit);
            return ids == null ? List.of() : ids;
        } catch (RuntimeException exception) {
            log.warn("종목뉴스 벡터 검색에 실패했습니다. newsId={}", newsId, exception);
            return List.of();
        }
    }

    private List<News> ordered(List<News> found, List<Long> ids) {
        List<News> rows = new ArrayList<>();
        for (Long id : ids) {
            found.stream().filter(news -> id.equals(news.getId())).findFirst().ifPresent(rows::add);
        }
        return real(rows);
    }

    private List<News> shuffled(List<News> pool, int size) {
        List<News> copy = new ArrayList<>(pool);
        for (int index = copy.size() - 1; index > 0; index--) {
            swap(copy, index, draw.applyAsInt(index + 1));
        }
        return copy.subList(0, Math.min(size, copy.size()));
    }

    private void swap(List<News> rows, int left, int right) {
        News current = rows.get(left);
        rows.set(left, rows.get(right));
        rows.set(right, current);
    }

    private List<News> newest(List<News> rows, int size) {
        return rows.stream()
                .sorted(Comparator.comparing(this::publishedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(size)
                .toList();
    }

    private List<News> real(List<News> rows) {
        if (rows == null) {
            return List.of();
        }
        return rows.stream().filter(this::published).toList();
    }

    private boolean published(News news) {
        String title = rawTitle(news);
        return news != null && news.getId() != null && (title == null || !title.startsWith("[더미]"));
    }

    private List<News> cap(List<News> rows) {
        if (rows.size() <= POOL) {
            return rows;
        }
        return rows.subList(0, POOL);
    }

    private EconomyPickNewsView view(String mode, List<News> rows) {
        return new EconomyPickNewsView(mode, rows.stream().map(this::item).toList());
    }

    private EconomyPickNewsView.Item item(News news) {
        return new EconomyPickNewsView.Item(
                news.getId(),
                titleOf(news),
                sourceOf(news),
                publishedAt(news) == null ? null : publishedAt(news).toString(),
                hrefOf(news),
                imageOf(news));
    }

    private String titleOf(News news) {
        String translated = text(news.getTranslatedContent());
        String original = text(news.getOriginalContent());
        String chosen = translated == null || translated.isBlank() ? original : translated;
        String repaired = NewsTextRepair.repair(chosen);
        return repaired == null || repaired.isBlank() ? "제목 없음" : repaired;
    }

    private String rawTitle(News news) {
        if (news == null) {
            return null;
        }
        String translated = text(news.getTranslatedContent());
        return translated == null || translated.isBlank() ? text(news.getOriginalContent()) : translated;
    }

    private String text(Content content) {
        return content == null ? null : content.getTitle();
    }

    private String sourceOf(News news) {
        NewsProvider provider = news.getNewsProvider();
        if (provider == null) {
            return "뉴스";
        }
        return switch (provider) {
            case BLOOMBERG -> "블룸버그";
            case MARKETAUX -> "마켓옥스";
            case ALPHA_VANTAGE -> "알파밴티지";
            case YAHOO_FINANCE -> "야후 파이낸스";
            default -> "뉴스";
        };
    }

    private String hrefOf(News news) {
        NewsMeta meta = news.getNewsMeta();
        String source = meta == null ? null : meta.getSourceUrl();
        if (source != null && (source.startsWith("http://") || source.startsWith("https://"))) {
            return source;
        }
        return "/news/" + news.getId();
    }

    private String imageOf(News news) {
        Content content = news.getOriginalContent();
        if (content == null || content.getImageUrl() == null || content.getImageUrl().isBlank()) {
            return null;
        }
        return content.getImageUrl();
    }

    private LocalDateTime publishedAt(News news) {
        NewsMeta meta = news.getNewsMeta();
        if (meta != null && meta.getNewsPublishedTime() != null) {
            return meta.getNewsPublishedTime();
        }
        return news.getScrapedTime();
    }

    private EconomyPickLens lens() {
        EconomyPickLens[] values = EconomyPickLens.values();
        return values[draw.applyAsInt(values.length)];
    }

    private String keyword(EconomyPickLens lens) {
        return lens.keywords[draw.applyAsInt(lens.keywords.length)];
    }

    private int clamp(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 20);
    }

    private enum EconomyPickLens {
        DASHBOARD("코스닥", "공시", "전환사채"),
        QUANT("엔비디아", "반도체", "나스닥"),
        SPACEX("우주", "항공", "방산"),
        TRUMP("트럼프", "관세", "정책");

        private final String[] keywords;

        EconomyPickLens(String... keywords) {
            this.keywords = keywords;
        }
    }
}
