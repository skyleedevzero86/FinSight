package com.sleekydz86.finsight.core.portfolio.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioShareCommentJpaRepository extends JpaRepository<PortfolioShareCommentJpaEntity, Long> {

    List<PortfolioShareCommentJpaEntity> findTop30ByShareIdOrderByIdDesc(Long shareId);
}
