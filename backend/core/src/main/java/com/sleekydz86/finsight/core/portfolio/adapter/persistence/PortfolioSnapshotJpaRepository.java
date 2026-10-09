package com.sleekydz86.finsight.core.portfolio.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortfolioSnapshotJpaRepository extends JpaRepository<PortfolioSnapshotJpaEntity, Long> {

    List<PortfolioSnapshotJpaEntity> findByUserIdOrderByYearMonthAsc(Long userId);

    Optional<PortfolioSnapshotJpaEntity> findByUserIdAndYearMonth(Long userId, String yearMonth);
}
