package com.sleekydz86.finsight.core.news.adapter.crawl;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NewsCrawlAttemptJpaRepository extends JpaRepository<NewsCrawlAttemptJpaEntity, Long> {

    List<NewsCrawlAttemptJpaEntity> findByIncidentIdOrderByIdAsc(long incidentId);
}
