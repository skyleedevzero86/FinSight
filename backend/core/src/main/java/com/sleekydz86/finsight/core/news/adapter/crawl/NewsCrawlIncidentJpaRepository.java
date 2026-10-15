package com.sleekydz86.finsight.core.news.adapter.crawl;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NewsCrawlIncidentJpaRepository extends JpaRepository<NewsCrawlIncidentJpaEntity, Long> {

    List<NewsCrawlIncidentJpaEntity> findTop50ByOrderByIdDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select incident from NewsCrawlIncidentJpaEntity incident where incident.id = :id")
    Optional<NewsCrawlIncidentJpaEntity> lockById(@Param("id") Long id);
}
