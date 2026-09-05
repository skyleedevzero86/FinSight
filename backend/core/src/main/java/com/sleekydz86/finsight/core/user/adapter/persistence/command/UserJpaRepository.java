package com.sleekydz86.finsight.core.user.adapter.persistence.command;

import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.user.domain.UserRole;
import com.sleekydz86.finsight.core.user.domain.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

        Optional<UserJpaEntity> findByEmail(String email);

        Optional<UserJpaEntity> findByUsername(String username);

        Optional<UserJpaEntity> findByApiKey(String apiKey);

        Optional<UserJpaEntity> findByNaverId(String naverId);

        Optional<UserJpaEntity> findByKakaoUserId(String kakaoUserId);

        Optional<UserJpaEntity> findByGoogleId(String googleId);

        boolean existsByEmail(String email);

        boolean existsByUsername(String username);

        boolean existsByApiKey(String apiKey);

        @Query("SELECT u FROM UserJpaEntity u WHERE u.status = 'SUSPENDED' AND u.accountLockedAt < :unlockTime")
        List<UserJpaEntity> findLockedUsersBeforeUnlockTime(@Param("unlockTime") LocalDateTime unlockTime);

        @Query("SELECT u FROM UserJpaEntity u WHERE u.status = :status AND u.createdAt > :after")
        List<UserJpaEntity> findPendingUsersAfter(@Param("status") UserStatus status,
                        @Param("after") LocalDateTime after);

        @Query("SELECT u FROM UserJpaEntity u WHERE u.passwordChangedAt < :before OR u.passwordChangedAt IS NULL")
        List<UserJpaEntity> findUsersWithPasswordChangedBefore(@Param("before") LocalDateTime before);

        @Query("SELECT COUNT(u) FROM UserJpaEntity u WHERE u.passwordChangedAt > :after")
        long countPasswordChangesAfter(@Param("after") LocalDateTime after);

        @Query("SELECT COUNT(u) FROM UserJpaEntity u WHERE u.passwordChangedAt < :before OR u.passwordChangedAt IS NULL")
        long countUsersWithPasswordChangedBefore(@Param("before") LocalDateTime before);

        Page<UserJpaEntity> findByStatus(UserStatus status, Pageable pageable);

        Page<UserJpaEntity> findByStatusAndRole(UserStatus status, UserRole role, Pageable pageable);

        long countByStatus(UserStatus status);

        @Query("SELECT DISTINCT u FROM UserJpaEntity u JOIN u.watchlist w WHERE w IN :categories")
        List<UserJpaEntity> findByWatchlistIn(@Param("categories") List<TargetCategory> categories);

        @Query("SELECT u FROM UserJpaEntity u WHERE u.status = 'APPROVED' AND (u.loginFailCount < 5 OR u.loginFailCount IS NULL)")
        List<UserJpaEntity> findAllActiveUsers();

        Optional<UserJpaEntity> findByEmailAndUsername(String email, String username);

        @Query("""
                SELECT u FROM UserJpaEntity u
                WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))
                """)
        Page<UserJpaEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

        @Query("""
                SELECT u FROM UserJpaEntity u
                WHERE u.status = :status
                  AND (
                    LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  )
                """)
        Page<UserJpaEntity> searchByStatusAndKeyword(
                @Param("status") UserStatus status,
                @Param("keyword") String keyword,
                Pageable pageable);

        @Query(value = """
                        SELECT DATE(`createdAt`) AS d, COUNT(*) AS c
                        FROM users
                        WHERE `createdAt` >= :from AND `createdAt` < :to
                        GROUP BY DATE(`createdAt`)
                        ORDER BY d
                        """, nativeQuery = true)
        List<Object[]> countSignupsByDay(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

        @Query(value = """
                        SELECT DATE(`lastLoginAt`) AS d, COUNT(*) AS c
                        FROM users
                        WHERE `lastLoginAt` IS NOT NULL
                          AND `lastLoginAt` >= :from AND `lastLoginAt` < :to
                        GROUP BY DATE(`lastLoginAt`)
                        ORDER BY d
                        """, nativeQuery = true)
        List<Object[]> countLoginsByDay(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

        @Query(value = """
                        SELECT auth_provider, COUNT(*) AS c
                        FROM users
                        GROUP BY auth_provider
                        """, nativeQuery = true)
        List<Object[]> countByAuthProvider();

        @Query(value = """
                        SELECT status, COUNT(*) AS c
                        FROM users
                        GROUP BY status
                        """, nativeQuery = true)
        List<Object[]> countGroupedByStatus();

        @Query(value = """
                        SELECT DATE(`updatedAt`) AS d, COUNT(*) AS c
                        FROM users
                        WHERE status = 'WITHDRAWN'
                          AND `updatedAt` >= :from AND `updatedAt` < :to
                        GROUP BY DATE(`updatedAt`)
                        ORDER BY d
                        """, nativeQuery = true)
        List<Object[]> countWithdrawnByUpdatedDay(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

        @Query(value = """
                        SELECT COUNT(*)
                        FROM users
                        WHERE `createdAt` < :before
                        """, nativeQuery = true)
        long countCreatedBefore(@Param("before") LocalDateTime before);
}