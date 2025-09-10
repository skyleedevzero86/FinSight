package com.sleekydz86.finsight.core.user.adapter.persistence.command;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserPasswordHistoryJpaRepository extends JpaRepository<UserPasswordHistoryJpaEntity, Long> {

    List<UserPasswordHistoryJpaEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT u FROM UserPasswordHistoryJpaEntity u WHERE u.user.id = :userId ORDER BY u.createdAt DESC")
    List<UserPasswordHistoryJpaEntity> findByUserOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT COUNT(u) FROM UserPasswordHistoryJpaEntity u WHERE u.user.id = :userId AND u.createdAt >= :since")
    long countByUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT u FROM UserPasswordHistoryJpaEntity u WHERE u.user.id = :userId ORDER BY u.createdAt DESC")
    List<UserPasswordHistoryJpaEntity> findRecentByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM UserPasswordHistoryJpaEntity u WHERE u.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}