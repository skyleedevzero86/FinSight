package com.sleekydz86.finsight.core.user.domain.port.out;

import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;

import java.util.List;
import java.util.Optional;

public interface UserPersistencePort {
    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByWatchlistCategories(List<TargetCategory> categories);

    void deleteById(Long id);
}