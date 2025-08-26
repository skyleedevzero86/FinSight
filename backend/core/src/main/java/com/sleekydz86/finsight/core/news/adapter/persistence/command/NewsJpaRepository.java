package com.sleekydz86.finsight.core.news.adapter.persistence.command;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NewsJpaRepository extends JpaRepository<NewsJpaEntity, Long> {

    List<NewsJpaEntity> findByOverviewIsNull();

    Page<NewsJpaEntity> findByOverviewIsNull(Pageable pageable);

    @Override
    List<NewsJpaEntity> findAll();
}