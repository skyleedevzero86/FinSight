package com.sleekydz86.finsight.core.user.adapter.persistence.command;

import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@Transactional(readOnly = true)
public class UserRepositoryImpl implements UserPersistencePort {

    private final UserJpaRepository userJpaRepository;
    private final UserJpaMapper userJpaMapper;

    public UserRepositoryImpl(UserJpaRepository userJpaRepository, UserJpaMapper userJpaMapper) {
        this.userJpaRepository = userJpaRepository;
        this.userJpaMapper = userJpaMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public User save(User user) {
        try {
            var entity = userJpaMapper.toEntity(user);
            var savedEntity = userJpaRepository.save(entity);
            return userJpaMapper.toDomain(savedEntity);
        } catch (Exception e) {
            throw new RuntimeException("사용자 저장 실패", e);
        }
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Optional<User> findById(Long id) {
        try {
            return userJpaRepository.findById(id)
                    .map(userJpaMapper::toDomain);
        } catch (Exception e) {
            throw new RuntimeException("사용자 조회 실패: " + id, e);
        }
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Optional<User> findByEmail(String email) {
        try {
            return userJpaRepository.findByEmail(email)
                    .map(userJpaMapper::toDomain);
        } catch (Exception e) {
            throw new RuntimeException("이메일로 사용자 조회 실패: " + email, e);
        }
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public boolean existsByEmail(String email) {
        try {
            return userJpaRepository.existsByEmail(email);
        } catch (Exception e) {
            throw new RuntimeException("이메일 존재 확인 실패: " + email, e);
        }
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<User> findByWatchlistCategories(List<TargetCategory> categories) {
        try {
            // N+1 쿼리 방지를 위한 단일 쿼리 사용
            List<UserJpaEntity> entities = userJpaRepository.findByWatchlistCategories(categories);
            return entities.stream()
                    .map(userJpaMapper::toDomain)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("워치리스트 카테고리로 사용자 조회 실패", e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public void deleteById(Long id) {
        try {
            userJpaRepository.deleteById(id);
        } catch (Exception e) {
            throw new RuntimeException("사용자 삭제 실패: " + id, e);
        }
    }
}