package com.sleekydz86.finsight.core.news.adapter.crawl;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NewsCrawlLockJpaRepository extends JpaRepository<NewsCrawlLockJpaEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select held from NewsCrawlLockJpaEntity held where held.provider = :provider")
    Optional<NewsCrawlLockJpaEntity> lockByProvider(@Param("provider") String provider);
}
