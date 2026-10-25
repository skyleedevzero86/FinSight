package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsDetailResponse;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
import com.sleekydz86.finsight.core.news.domain.vo.NewsTextRepair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Service
public class StoredNewsTranslationService {

    static final int MAX_PER_REQUEST = 12;
    private static final long RETRY_WAIT_MILLIS = 20_000L;

    private static final Logger log = LoggerFactory.getLogger(StoredNewsTranslationService.class);

    private final NewsAiProcessingService newsAiProcessingService;
    private final NewsPersistencePort newsPersistencePort;
    private final Executor executor;
    private final Set<Long> busy = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<Long, Long> retryAfter = new ConcurrentHashMap<>();

    public StoredNewsTranslationService(NewsAiProcessingService newsAiProcessingService,
                                        NewsPersistencePort newsPersistencePort,
                                        @Qualifier("newsTranslationExecutor") Executor newsTranslationExecutor) {
        this.newsAiProcessingService = newsAiProcessingService;
        this.newsPersistencePort = newsPersistencePort;
        this.executor = newsTranslationExecutor;
    }

    public Newses fill(Newses source) {
        if (source == null) {
            return new Newses();
        }
        queueTranslation(source.getNewses());
        return source;
    }

    public NewsDetailResponse fill(NewsDetailResponse detail) {
        if (detail == null || detail.getId() == null) {
            return detail;
        }
        newsPersistencePort.findById(detail.getId())
                .ifPresent(news -> queueTranslation(List.of(news)));
        return detail;
    }

    private void queueTranslation(List<News> news) {
        List<News> pending = nextBatch(news);
        if (!pending.isEmpty()) {
            executor.execute(() -> translate(pending));
        }
    }

    private List<News> nextBatch(List<News> news) {
        List<News> pending = new ArrayList<>();
        if (news == null) {
            return pending;
        }
        for (News item : news) {
            if (pending.size() >= MAX_PER_REQUEST) {
                return pending;
            }
            if (needsKoreanTitle(item) && !cooling(item.getId()) && busy.add(item.getId())) {
                pending.add(item);
            }
        }
        return pending;
    }

    private boolean cooling(Long id) {
        Long until = retryAfter.get(id);
        if (until == null) {
            return false;
        }
        if (until <= System.currentTimeMillis()) {
            retryAfter.remove(id);
            return false;
        }
        return true;
    }

    private void translate(List<News> pending) {
        int stored = 0;
        try {
            for (News news : pending) {
                if (storeOne(news)) {
                    stored++;
                }
            }
        } finally {
            pending.forEach(news -> busy.remove(news.getId()));
            log.info("저장된 뉴스 번역을 반영했습니다 요청={} 반영={}", pending.size(), stored);
        }
    }

    private boolean storeOne(News news) {
        try {
            News analyzed = newsAiProcessingService.processNewsWithAI(news);
            if (storeIfKorean(analyzed)) {
                retryAfter.remove(news.getId());
                return true;
            }
        } catch (RuntimeException exception) {
            log.warn("저장된 뉴스 번역에 실패했습니다 id={}", news.getId(), exception);
        }
        cool(news.getId());
        return false;
    }

    private void cool(Long id) {
        if (id != null) {
            retryAfter.put(id, System.currentTimeMillis() + RETRY_WAIT_MILLIS);
        }
    }

    private boolean storeIfKorean(News news) {
        if (!readableKorean(translatedTitle(news), originalTitle(news))) {
            return false;
        }
        try {
            newsPersistencePort.saveStoredAnalysis(news);
            return true;
        } catch (RuntimeException exception) {
            log.warn("저장된 뉴스 번역을 반영하지 못했습니다 id={}", news.getId(), exception);
            return false;
        }
    }

    static boolean needsKoreanTitle(News news) {
        if (news == null || news.getId() == null || news.getId() <= 0 || news.getOriginalContent() == null) {
            return false;
        }
        return needsTitle(translatedTitle(news), news.getOriginalContent().getTitle());
    }

    static boolean readableKorean(String translated, String original) {
        if (needsTitle(translated, original)) {
            return false;
        }
        String text = repaired(translated);
        Set<Integer> syllables = new HashSet<>();
        int hangul = 0;
        for (int index = 0; index < text.length(); ) {
            int code = text.codePointAt(index);
            index += Character.charCount(code);
            if (code < 0xAC00 || code > 0xD7A3) {
                continue;
            }
            hangul++;
            syllables.add(code);
        }
        if (hangul < 2) {
            return false;
        }
        return hangul < 6 || syllables.size() > 2;
    }

    private static String translatedTitle(News news) {
        if (news.getTranslatedContent() == null) {
            return "";
        }
        return news.getTranslatedContent().getTitle();
    }

    private static String originalTitle(News news) {
        if (news.getOriginalContent() == null) {
            return "";
        }
        return news.getOriginalContent().getTitle();
    }

    private static boolean needsTitle(String translated, String original) {
        String source = repaired(original);
        if (source.isBlank()) {
            return false;
        }
        String korean = repaired(translated);
        return korean.isBlank() || korean.equals(source);
    }

    private static String repaired(String value) {
        String repaired = NewsTextRepair.repair(value);
        return repaired == null ? "" : repaired.trim();
    }
}
