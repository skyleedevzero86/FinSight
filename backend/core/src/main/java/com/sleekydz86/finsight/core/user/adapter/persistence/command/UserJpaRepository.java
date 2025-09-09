package com.sleekydz86.finsight.core.user.adapter.persistence.command;

import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.UserRole;
import com.sleekydz86.finsight.core.user.domain.UserStatus;
import com.sleekydz86.finsight.core.user.domain.port.out.dto.UserStatsDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserJpaRepository extends JpaRepository<User, Long> {

    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_CACHEABLE, value = "true")
    })
    Optional<User> findByUsername(String username);

    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_CACHEABLE, value = "true")
    })
    Optional<User> findByEmail(String email);

    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_CACHEABLE, value = "true")
    })
    Optional<User> findByApiKey(String apiKey);

    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_CACHEABLE, value = "true")
    })
    boolean existsByUsername(String username);

    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_CACHEABLE, value = "true")
    })
    boolean existsByEmail(String email);

    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_CACHEABLE, value = "true")
    })
    Page<User> findByStatus(UserStatus status, Pageable pageable);

    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_CACHEABLE, value = "true")
    })
    Page<User> findByRole(UserRole role, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.status = :status AND u.createDate >= :fromDate")
    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_CACHEABLE, value = "true")
    })
    List<User> findPendingUsersAfter(@Param("status") UserStatus status,
                                     @Param("fromDate") LocalDateTime fromDate);

    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status")
    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_CACHEABLE, value = "true")
    })
    long countByStatus(@Param("status") UserStatus status);

    @Query("SELECT u FROM User u WHERE u.accountLockedAt IS NOT NULL AND u.accountLockedAt < :unlockTime")
    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_FETCH_SIZE, value = "100")
    })
    List<User> findLockedUsersBeforeUnlockTime(@Param("unlockTime") LocalDateTime unlockTime);

    @Query("SELECT u FROM User u WHERE u.passwordChangedAt < :beforeDate AND u.status = 'APPROVED'")
    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_FETCH_SIZE, value = "100")
    })
    List<User> findUsersWithPasswordChangedBefore(@Param("beforeDate") LocalDateTime beforeDate);

    @Query("SELECT COUNT(u) FROM User u WHERE u.passwordChangedAt < :beforeDate AND u.status = 'APPROVED'")
    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_CACHEABLE, value = "true")
    })
    long countUsersWithPasswordChangedBefore(@Param("beforeDate") LocalDateTime beforeDate);

    @Query("SELECT COUNT(uph) FROM UserPasswordHistory uph WHERE uph.createdAt >= :fromDate")
    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_CACHEABLE, value = "true")
    })
    long countPasswordChangesAfter(@Param("fromDate") LocalDateTime fromDate);

    @Query("SELECT u FROM User u WHERE u.passwordChangedAt IS NULL AND u.status = 'APPROVED'")
    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_FETCH_SIZE, value = "100")
    })
    List<User> findUsersWithNullPasswordChangedAt();

    @Query("SELECT COUNT(u) FROM User u WHERE u.passwordChangedAt IS NULL AND u.status = 'APPROVED'")
    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_CACHEABLE, value = "true")
    })
    long countUsersWithNullPasswordChangedAt();

    @Query("SELECT u FROM User u WHERE u.lastPasswordChangeDate = :date")
    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_FETCH_SIZE, value = "100")
    })
    List<User> findUsersByLastPasswordChangeDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(u) FROM User u WHERE u.lastPasswordChangeDate = :date AND u.passwordChangeCount >= :count")
    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_CACHEABLE, value = "true")
    })
    long countUsersExceedingDailyLimit(@Param("date") LocalDate date, @Param("count") int count);

    @Modifying
    @Query(value = "UPDATE users SET login_fail_count = :failCount, status = :status, account_locked_at = :lockedAt, modify_date = NOW() WHERE id = :userId", nativeQuery = true)
    int updateLoginFailBySql(@Param("userId") Long userId,
                             @Param("failCount") Integer failCount,
                             @Param("status") String status,
                             @Param("lockedAt") LocalDateTime lockedAt);

    @Modifying
    @Query(value = "UPDATE users SET login_fail_count = :failCount, modify_date = NOW() WHERE id = :userId", nativeQuery = true)
    int updateLoginFailCountOnly(@Param("userId") Long userId, @Param("failCount") Integer failCount);

    @Query(value = "SELECT login_fail_count FROM users WHERE id = :userId", nativeQuery = true)
    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_CACHEABLE, value = "true")
    })
    Integer getCurrentLoginFailCount(@Param("userId") Long userId);

    @Query("SELECT u FROM User u WHERE (:status IS NULL OR u.status = :status) AND (:role IS NULL OR u.role = :role)")
    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_CACHEABLE, value = "true")
    })
    Page<User> findByStatusAndRole(@Param("status") UserStatus status,
                                   @Param("role") UserRole role,
                                   Pageable pageable);

    @Query("SELECT u.id FROM User u WHERE u.id IN :ids")
    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_FETCH_SIZE, value = "1000")
    })
    List<Long> findIdsByIdIn(@Param("ids") List<Long> ids);

    @Query("SELECT NEW com.sleekydz86.finsight.core.user.domain.port.in.dto.UserStatsDto(u.status, COUNT(u)) FROM User u GROUP BY u.status")
    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_CACHEABLE, value = "true")
    })
    List<UserStatsDto> getUserStats();

    @Modifying
    @Query(value = "UPDATE users SET login_fail_count = 0, last_login_at = :lastLoginAt, modify_date = NOW() WHERE id = :userId", nativeQuery = true)
    int updateLoginSuccessBySql(@Param("userId") Long userId,
                                @Param("lastLoginAt") LocalDateTime lastLoginAt);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_CACHEABLE, value = "true")
    })
    long countByRole(@Param("role") UserRole role);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.status = :status")
    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_CACHEABLE, value = "true")
    })
    long countByRoleAndStatus(@Param("role") UserRole role, @Param("status") UserStatus status);

    @Query("SELECT u FROM User u WHERE u.accountLockedAt IS NOT NULL AND u.accountLockedAt < :unlockTime")
    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_FETCH_SIZE, value = "100")
    })
    List<User> findLockedUsersBeforeUnlockTime(@Param("unlockTime") LocalDateTime unlockTime);

    @Query("SELECT u FROM User u WHERE u.passwordChangedAt < :beforeDate AND u.status = 'APPROVED'")
    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_FETCH_SIZE, value = "100")
    })
    List<User> findUsersWithPasswordChangedBefore(@Param("beforeDate") LocalDateTime beforeDate);

    @Query("SELECT COUNT(u) FROM User u WHERE u.passwordChangedAt < :beforeDate AND u.status = 'APPROVED'")
    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_CACHEABLE, value = "true")
    })
    long countUsersWithPasswordChangedBefore(@Param("beforeDate") LocalDateTime beforeDate);

    @Query("SELECT COUNT(uph) FROM UserPasswordHistory uph WHERE uph.createdAt >= :fromDate")
    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_CACHEABLE, value = "true")
    })
    long countPasswordChangesAfter(@Param("fromDate") LocalDateTime fromDate);
}
