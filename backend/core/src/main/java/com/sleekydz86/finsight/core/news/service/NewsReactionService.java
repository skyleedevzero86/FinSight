package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.global.exception.NewsNotFoundException;
import com.sleekydz86.finsight.core.global.exception.ValidationException;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsReactionJpaEntity;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsReactionJpaRepository;
import com.sleekydz86.finsight.core.news.domain.NewsStatistics;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsReactionResponse;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsStatisticsPersistencePort;
import com.sleekydz86.finsight.core.news.domain.vo.NewsReactionChoice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class NewsReactionService {

    private final NewsReactionJpaRepository reactions;
    private final NewsPersistencePort newsPersistencePort;
    private final NewsStatisticsPersistencePort statisticsPort;

    public NewsReactionService(NewsReactionJpaRepository reactions,
                               NewsPersistencePort newsPersistencePort,
                               NewsStatisticsPersistencePort statisticsPort) {
        this.reactions = reactions;
        this.newsPersistencePort = newsPersistencePort;
        this.statisticsPort = statisticsPort;
    }

    @Transactional(readOnly = true)
    public NewsReactionResponse view(Long newsId, String email) {
        requireNews(newsId);
        return snapshot(newsId, email);
    }

    @Transactional
    public NewsReactionResponse toggle(Long newsId, String email, String requested) {
        requireNews(newsId);
        requireUser(email);
        apply(newsId, email.trim(), NewsReactionChoice.normalize(requested));
        syncStatistics(newsId);
        return snapshot(newsId, email);
    }

    private void requireNews(Long newsId) {
        if (newsId == null || newsId <= 0 || newsPersistencePort.findById(newsId).isEmpty()) {
            throw new NewsNotFoundException(newsId);
        }
    }

    private void requireUser(String email) {
        if (email == null || email.isBlank()) {
            throw new ValidationException("로그인이 필요합니다.", List.of("user"));
        }
    }

    private void apply(Long newsId, String email, String requested) {
        Optional<NewsReactionJpaEntity> existing = reactions.findByUserEmailAndNewsId(email, newsId);
        String next = NewsReactionChoice.apply(existing.map(NewsReactionJpaEntity::getReactionType).orElse(null), requested);
        if (existing.isEmpty()) {
            reactions.save(new NewsReactionJpaEntity(newsId, email, next));
            return;
        }
        NewsReactionJpaEntity row = existing.get();
        if (next == null) {
            reactions.delete(row);
            return;
        }
        row.setReactionType(next);
        reactions.save(row);
    }

    private void syncStatistics(Long newsId) {
        int likes = count(newsId, "LIKE");
        int dislikes = count(newsId, "DISLIKE");
        NewsStatistics current = statisticsPort.findByNewsId(newsId).orElse(null);
        statisticsPort.save(copyCounts(newsId, current, likes, dislikes));
    }

    private NewsReactionResponse snapshot(Long newsId, String email) {
        String mine = mine(newsId, email);
        return new NewsReactionResponse(mine, count(newsId, "LIKE"), count(newsId, "DISLIKE"));
    }

    private String mine(Long newsId, String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return reactions.findByUserEmailAndNewsId(email.trim(), newsId)
                .map(NewsReactionJpaEntity::getReactionType)
                .orElse(null);
    }

    private int count(Long newsId, String type) {
        return (int) reactions.countByNewsIdAndReactionType(newsId, type);
    }

    private NewsStatistics copyCounts(Long newsId, NewsStatistics current, int likes, int dislikes) {
        if (current == null) {
            return NewsStatistics.builder().newsId(newsId).likeCount(likes).dislikeCount(dislikes).build();
        }
        return NewsStatistics.builder()
                .id(current.getId())
                .newsId(newsId)
                .viewCount(current.getViewCount())
                .likeCount(likes)
                .dislikeCount(dislikes)
                .commentCount(current.getCommentCount())
                .createdAt(current.getCreatedAt())
                .updatedAt(current.getUpdatedAt())
                .build();
    }
}
