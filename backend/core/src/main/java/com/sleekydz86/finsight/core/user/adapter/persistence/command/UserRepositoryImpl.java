package com.sleekydz86.finsight.core.user.adapter.persistence.command;

import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserPersistencePort {

    private final UserJpaRepository userJpaRepository;
    private final UserJpaMapper userJpaMapper;

    public UserRepositoryImpl(UserJpaRepository userJpaRepository, UserJpaMapper userJpaMapper) {
        this.userJpaRepository = userJpaRepository;
        this.userJpaMapper = userJpaMapper;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = userJpaMapper.toEntity(user);
        UserJpaEntity savedEntity = userJpaRepository.save(entity);
        return userJpaMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id)
                .map(userJpaMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email)
                .map(userJpaMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public List<User> findByWatchlistCategories(List<TargetCategory> categories) {
        return userJpaRepository.findByWatchlistCategories(categories)
                .stream()
                .map(userJpaMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        userJpaRepository.deleteById(id);
    }
}