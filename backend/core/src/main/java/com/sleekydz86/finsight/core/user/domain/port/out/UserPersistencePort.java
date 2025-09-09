// core/src/main/java/com/sleekydz86/finsight/core/user/domain/port/out/UserPersistencePort.java
package com.sleekydz86.finsight.core.user.domain.port.out;

import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.UserStatus;

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
    void deleteById(Long id);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByApiKey(String apiKey);

    List<User> findLockedUsersBeforeUnlockTime(LocalDateTime unlockTime);
    List<User> findPendingUsersAfter(UserStatus status, LocalDateTime after);
    List<User> findUsersWithPasswordChangedBefore(LocalDateTime before);
    long countPasswordChangesAfter(LocalDateTime after);
    long countUsersWithPasswordChangedBefore(LocalDateTime before);
}