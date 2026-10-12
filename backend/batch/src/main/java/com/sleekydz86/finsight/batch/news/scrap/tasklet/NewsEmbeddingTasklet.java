package com.sleekydz86.finsight.batch.news.scrap.tasklet;

import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaEntity;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaRepository;
import com.sleekydz86.finsight.core.news.domain.vo.EmbeddingStatus;
import com.sleekydz86.finsight.core.news.service.NewsVectorPublishService;
import com.sleekydz86.finsight.core.news.service.NewsVectorPublishService.PublishResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class NewsEmbeddingTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(NewsEmbeddingTasklet.class);
    private static final int PAGE = 20;
    private static final int CAP = 200;
    private static final int MAX_ATTEMPTS = 3;

    private final NewsJpaRepository newsJpaRepository;
    private final NewsVectorPublishService publishService;

    public NewsEmbeddingTasklet(NewsJpaRepository newsJpaRepository,
                                NewsVectorPublishService publishService) {
        this.newsJpaRepository = newsJpaRepository;
        this.publishService = publishService;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        if (!publishService.enabled()) {
            log.info("뉴스 임베딩이 비활성화되어 단계를 건너뜁니다.");
            return RepeatStatus.FINISHED;
        }
        if (!publishService.ready()) {
            log.warn("뉴스 임베딩 모델을 사용할 수 없습니다. 실패로 기록하고 다음 실행에서 재시도합니다.");
            markUnavailable(newsJpaRepository.findEmbeddingPendingIds(
                    EmbeddingStatus.PENDING, PageRequest.of(0, PAGE)));
            return RepeatStatus.FINISHED;
        }
        int indexed = drain();
        int retried = retry();
        log.info("뉴스 임베딩 단계 완료 - 색인: {}, 재시도: {}", indexed, retried);
        return RepeatStatus.FINISHED;
    }

    private int drain() {
        int seen = 0;
        while (seen < CAP) {
            List<Long> ids = newsJpaRepository.findEmbeddingPendingIds(
                    EmbeddingStatus.PENDING, PageRequest.of(0, PAGE));
            if (ids.isEmpty() || !publishIds(ids)) {
                return seen;
            }
            seen += ids.size();
        }
        return seen;
    }

    private int retry() {
        List<Long> ids = newsJpaRepository.findEmbeddingRetryIds(
                EmbeddingStatus.FAILED, MAX_ATTEMPTS, PageRequest.of(0, PAGE));
        publishIds(ids);
        return ids.size();
    }

    private boolean publishIds(List<Long> ids) {
        for (Long id : ids) {
            NewsJpaEntity news = newsJpaRepository.findById(id).orElse(null);
            if (news == null) {
                continue;
            }
            PublishResult result = publishService.publish(news);
            if (result == PublishResult.CACHED_ONLY) {
                return false;
            }
            apply(news, result);
            newsJpaRepository.save(news);
        }
        return true;
    }

    private void markUnavailable(List<Long> ids) {
        for (Long id : ids) {
            newsJpaRepository.findById(id).ifPresent(news -> {
                apply(news, PublishResult.FAILED);
                newsJpaRepository.save(news);
            });
        }
    }

    private void apply(NewsJpaEntity news, PublishResult result) {
        news.setEmbeddingModel(publishService.modelName());
        news.setUpdatedAt(LocalDateTime.now());
        if (result == PublishResult.INDEXED) {
            news.setEmbeddingStatus(EmbeddingStatus.DONE);
            return;
        }
        news.setEmbeddingStatus(EmbeddingStatus.FAILED);
        news.setEmbeddingAttempts(news.getEmbeddingAttempts() + 1);
    }
}
