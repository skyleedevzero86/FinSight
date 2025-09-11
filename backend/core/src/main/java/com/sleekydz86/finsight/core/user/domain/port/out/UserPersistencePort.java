package com.sleekydz86.finsight.core.user.domain.port.out;

import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.UserRole;
import com.sleekydz86.finsight.core.user.domain.UserStatus;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserPersistencePort {
    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByApiKey(String apiKey);

    List<User> findAll();

    Page<User> findAll(Pageable pageable);

    void deleteById(Long id);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByApiKey(String apiKey);

    List<User> findLockedUsersBeforeUnlockTime(LocalDateTime unlockTime);

    List<User> findPendingUsersAfter(UserStatus status, LocalDateTime after);

    List<User> findUsersWithPasswordChangedBefore(LocalDateTime before);

    long countPasswordChangesAfter(LocalDateTime after);

    long countUsersWithPasswordChangedBefore(LocalDateTime before);

    Page<User> findByStatus(UserStatus status, Pageable pageable);

    Page<User> findByStatusAndRole(UserStatus status, UserRole role, Pageable pageable);

    long countByStatus(UserStatus status);

    List<User> findByWatchlistCategories(List<TargetCategory> categories);

    List<User> findAllActiveUsers();

    Optional<User> findByEmailAndUsername(String email, String username);
}