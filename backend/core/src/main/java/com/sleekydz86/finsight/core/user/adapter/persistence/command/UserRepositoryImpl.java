package com.sleekydz86.finsight.core.user.adapter.persistence.command;

import com.sleekydz86.finsight.core.user.adapter.persistence.command.UserJpaEntity;
import com.sleekydz86.finsight.core.user.adapter.persistence.command.UserJpaMapper;
import com.sleekydz86.finsight.core.user.adapter.persistence.command.UserJpaRepository;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.user.domain.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class UserRepositoryImpl implements UserPersistencePort {

    private static final Logger log = LoggerFactory.getLogger(UserRepositoryImpl.class);

    private final UserJpaRepository userJpaRepository;
    private final UserJpaMapper userJpaMapper;

    public UserRepositoryImpl(UserJpaRepository userJpaRepository, UserJpaMapper userJpaMapper) {
        this.userJpaRepository = userJpaRepository;
        this.userJpaMapper = userJpaMapper;
    }

    @Override
    public User save(User user) {
        log.debug("사용자 저장: {}", user.getEmail());

        UserJpaEntity entity = userJpaMapper.toEntity(user);
        entity.setUpdatedAt(LocalDateTime.now());

        UserJpaEntity savedEntity = userJpaRepository.save(entity);
        log.debug("사용자 저장 완료: {} (ID: {})", user.getEmail(), savedEntity.getId());

        return userJpaMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<User> findById(Long id) {
        log.debug("사용자 ID로 조회: {}", id);

        return userJpaRepository.findById(id)
                .map(userJpaMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        log.debug("사용자 이메일로 조회: {}", email);

        return userJpaRepository.findByEmail(email)
                .map(userJpaMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    public List<User> findAllActiveUsers() {
        log.debug("활성 사용자 목록 조회");

        return userJpaRepository.findAllActiveUsers()
                .stream()
                .map(userJpaMapper::toDomain)
                .toList();
    }

    @Override
    public List<User> findByWatchlistCategories(List<TargetCategory> categories) {
        log.debug("관심 종목으로 사용자 조회: {}", categories);

        return userJpaRepository.findByWatchlistCategories(categories)
                .stream()
                .map(userJpaMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        log.debug("사용자 삭제: {}", id);
        userJpaRepository.deleteById(id);
    }

    public List<User> findAll() {
        log.debug("전체 사용자 목록 조회");

        return userJpaRepository.findAll()
                .stream()
                .map(userJpaMapper::toDomain)
                .toList();
    }
}