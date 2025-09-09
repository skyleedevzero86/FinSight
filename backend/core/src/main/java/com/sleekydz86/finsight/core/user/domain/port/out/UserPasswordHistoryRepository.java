package com.sleekydz86.finsight.core.user.domain.port.out;

import com.sleekydz86.finsight.core.user.domain.UserPasswordHistory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserPasswordHistoryRepository extends JpaRepository<UserPasswordHistory, Long> {

    @Query("SELECT uph FROM UserPasswordHistory uph " +
            "WHERE uph.user.id = :userId " +
            "ORDER BY uph.createdAt DESC")
    List<UserPasswordHistory> findRecentPasswordHistoryWithLimit(
            @Param("userId") Long userId,
            Pageable pageable);

    default List<UserPasswordHistory> findRecentPasswordHistoryWithLimit(Long userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return findRecentPasswordHistoryWithLimit(userId, pageable);
    }

    @Query("SELECT COUNT(uph) FROM UserPasswordHistory uph " +
            "WHERE uph.user.id = :userId " +
            "AND uph.createdAt >= :afterDateTime")
    long countPasswordChangesAfter(
            @Param("userId") Long userId,
            @Param("afterDateTime") LocalDateTime afterDateTime);

    @Query("SELECT uph FROM UserPasswordHistory uph " +
            "WHERE uph.user.id = :userId " +
            "ORDER BY uph.createdAt DESC")
    List<UserPasswordHistory> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT COUNT(uph) FROM UserPasswordHistory uph " +
            "WHERE uph.user.id = :userId " +
            "AND DATE(uph.createdAt) = DATE(:today)")
    long countTodayPasswordChanges(
            @Param("userId") Long userId,
            @Param("today") LocalDateTime today);
}