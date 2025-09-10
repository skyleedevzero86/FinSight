package com.sleekydz86.finsight.core.user.adapter.persistence.command;

import com.sleekydz86.finsight.core.user.domain.UserPasswordHistory;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPasswordHistoryRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserPasswordHistoryRepositoryImpl implements UserPasswordHistoryRepository {

    private final UserPasswordHistoryJpaRepository userPasswordHistoryJpaRepository;
    private final UserPasswordHistoryJpaMapper userPasswordHistoryJpaMapper;
    private final UserJpaRepository userJpaRepository;

    public UserPasswordHistoryRepositoryImpl(UserPasswordHistoryJpaRepository userPasswordHistoryJpaRepository,
            UserPasswordHistoryJpaMapper userPasswordHistoryJpaMapper,
            UserJpaRepository userJpaRepository) {
        this.userPasswordHistoryJpaRepository = userPasswordHistoryJpaRepository;
        this.userPasswordHistoryJpaMapper = userPasswordHistoryJpaMapper;
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public UserPasswordHistory save(UserPasswordHistory passwordHistory) {
        UserJpaEntity user = userJpaRepository.findById(passwordHistory.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserPasswordHistoryJpaEntity entity = userPasswordHistoryJpaMapper.toEntity(passwordHistory, user);
        UserPasswordHistoryJpaEntity savedEntity = userPasswordHistoryJpaRepository.save(entity);
        return userPasswordHistoryJpaMapper.toDomain(savedEntity);
    }

    @Override
    public List<UserPasswordHistory> findByUserId(Long userId) {
        List<UserPasswordHistoryJpaEntity> entities = userPasswordHistoryJpaRepository
                .findByUserIdOrderByCreatedAtDesc(userId);
        return entities.stream()
                .map(userPasswordHistoryJpaMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserPasswordHistory> findRecentByUserId(Long userId, int limit) {
        List<UserPasswordHistoryJpaEntity> entities = userPasswordHistoryJpaRepository.findRecentByUserId(userId);
        return entities.stream()
                .limit(limit)
                .map(userPasswordHistoryJpaMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserPasswordHistory> findRecentPasswordHistoryWithLimit(Long userId, int limit) {
        return findRecentByUserId(userId, limit);
    }

    @Override
    public long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime since) {
        return userPasswordHistoryJpaRepository.countByUserIdAndCreatedAtAfter(userId, since);
    }

    @Override
    public long countPasswordChangesAfter(Long userId, LocalDateTime afterDateTime) {
        return countByUserIdAndCreatedAtAfter(userId, afterDateTime);
    }

    @Override
    public long countTodayPasswordChanges(Long userId, LocalDateTime today) {
        LocalDateTime startOfDay = today.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = today.toLocalDate().atTime(23, 59, 59);
        return userPasswordHistoryJpaRepository.countByUserIdAndCreatedAtAfter(userId, startOfDay) -
                userPasswordHistoryJpaRepository.countByUserIdAndCreatedAtAfter(userId, endOfDay);
    }

    @Override
    public void deleteById(Long id) {
        userPasswordHistoryJpaRepository.deleteById(id);
    }

    @Override
    public void deleteByUserId(Long userId) {
        userPasswordHistoryJpaRepository.deleteByUserId(userId);
    }
}