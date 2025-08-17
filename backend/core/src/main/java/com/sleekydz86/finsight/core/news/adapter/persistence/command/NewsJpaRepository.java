package com.sleekydz86.finsight.core.news.adapter.persistence.command;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NewsJpaRepository extends JpaRepository<NewsJpaEntity, Long> {

    List<NewsJpaEntity> findByOverviewIsNull();

    @Override
    List<NewsJpaEntity> findAll();
}