package com.sleekydz86.finsight.core.portfolio.adapter.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PortfolioShareJpaRepository extends JpaRepository<PortfolioShareJpaEntity, Long> {

    Page<PortfolioShareJpaEntity> findByVisibilityOrderByCreatedAtDescIdDesc(String visibility, Pageable pageable);

    List<PortfolioShareJpaEntity> findByVisibilityAndCreatedAtGreaterThanEqual(String visibility, LocalDateTime createdAt);
}
