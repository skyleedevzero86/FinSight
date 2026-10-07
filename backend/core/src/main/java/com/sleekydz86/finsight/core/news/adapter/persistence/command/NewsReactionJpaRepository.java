package com.sleekydz86.finsight.core.news.adapter.persistence.command;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NewsReactionJpaRepository extends JpaRepository<NewsReactionJpaEntity, Long> {

    Optional<NewsReactionJpaEntity> findByUserEmailAndNewsId(String userEmail, Long newsId);

    long countByNewsIdAndReactionType(Long newsId, String reactionType);
}
