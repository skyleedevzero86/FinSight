package com.sleekydz86.finsight.core.portfolio.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioShareGoalJpaRepository extends JpaRepository<PortfolioShareGoalJpaEntity, Long> {

    List<PortfolioShareGoalJpaEntity> findAllByOrderBySortOrderAsc();
}
