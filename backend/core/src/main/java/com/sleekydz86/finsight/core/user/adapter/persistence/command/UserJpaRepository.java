package com.sleekydz86.finsight.core.user.adapter.persistence.command;

import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM UserJpaEntity u WHERE u.isActive = true")
    List<UserJpaEntity> findAllActiveUsers();

    @Query("SELECT u FROM UserJpaEntity u WHERE u.watchlist IN :categories")
    List<UserJpaEntity> findByWatchlistCategories(@Param("categories") List<TargetCategory> categories);
}