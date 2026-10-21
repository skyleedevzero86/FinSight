package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsDetailResponse;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
import com.sleekydz86.finsight.core.news.domain.vo.NewsTextRepair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Service
public class StoredNewsTranslationService {

    static final int MAX_PER_REQUEST = 12;

    private static final Logger log = LoggerFactory.getLogger(StoredNewsTranslationService.class);

    private final NewsAiProcessingService newsAiProcessingService;
    private final NewsPersistencePort newsPersistencePort;
    private final Executor executor;
    private final Set<Long> busy = ConcurrentHashMap.newKeySet();
    private final Set<Long> skipped = ConcurrentHashMap.newKeySet();

    public StoredNewsTranslationService(NewsAiProcessingService newsAiProcessingService,
                                        NewsPersistencePort newsPersistencePort) {
        this(newsAiProcessingService, newsPersistencePort, task -> CompletableFuture.runAsync(task));
    }

    StoredNewsTranslationService(NewsAiProcessingService newsAiProcessingService,
                                 NewsPersistencePort newsPersistencePort,
                                 Executor executor) {
        this.newsAiProcessingService = newsAiProcessingService;
        this.newsPersistencePort = newsPersistencePort;
        this.executor = executor;
    }

    public Newses fill(Newses source) {
        if (source == null) {
            return new Newses();
        }
        List<News> pending = nextBatch(source.getNewses());
        if (!pending.isEmpty()) {
            executor.execute(() -> translate(pending));
        }
        return source;
    }

    public NewsDetailResponse fill(NewsDetailResponse detail) {
        if (detail == null || detail.getId() == null || !needsTitle(detail.getTranslatedTitle(), detail.getOriginalTitle())) {
            return detail;
        }
        newsPersistencePort.findById(detail.getId()).ifPresent(news -> fill(new Newses(List.of(news))));
        return detail;
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
            if (needsKoreanTitle(item) && !skipped.contains(item.getId()) && busy.add(item.getId())) {
                pending.add(item);
            }
        }
        return pending;
    }

    private void translate(List<News> pending) {
        try {
            List<News> analyzed = newsAiProcessingService.processNewsWithAI(pending);
            remember(pending, analyzed);
        } catch (RuntimeException exception) {
            log.warn("저장된 뉴스 번역에 실패했습니다 건수={}", pending.size(), exception);
            pending.forEach(news -> skipped.add(news.getId()));
        } finally {
            pending.forEach(news -> busy.remove(news.getId()));
        }
    }

    private void remember(List<News> pending, List<News> analyzed) {
        int stored = 0;
        for (int index = 0; index < pending.size(); index++) {
            News source = pending.get(index);
            News result = index < analyzed.size() ? analyzed.get(index) : source;
            if (storeIfKorean(result)) {
                stored++;
            } else {
                skipped.add(source.getId());
            }
        }
        log.info("저장된 뉴스 번역을 반영했습니다 요청={} 반영={}", pending.size(), stored);
    }

    private boolean storeIfKorean(News news) {
        if (needsKoreanTitle(news)) {
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

    private static String translatedTitle(News news) {
        if (news.getTranslatedContent() == null) {
            return "";
        }
        return news.getTranslatedContent().getTitle();
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
