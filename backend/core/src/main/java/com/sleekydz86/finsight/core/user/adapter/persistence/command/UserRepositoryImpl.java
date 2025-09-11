package com.sleekydz86.finsight.core.user.adapter.persistence.command;

import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.UserRole;
import com.sleekydz86.finsight.core.user.domain.UserStatus;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@Transactional
@Slf4j
public class UserRepositoryImpl implements UserPersistencePort {

    private final UserJpaRepository userJpaRepository;
    private final UserJpaMapper userJpaMapper;

    public UserRepositoryImpl(UserJpaRepository userJpaRepository, UserJpaMapper userJpaMapper) {
        this.userJpaRepository = userJpaRepository;
        this.userJpaMapper = userJpaMapper;
    }

    @Override
    public User save(User user) {
        try {
            UserJpaEntity entity = userJpaMapper.toEntity(user);
            UserJpaEntity savedEntity = userJpaRepository.save(entity);
            return userJpaMapper.toDomain(savedEntity);
        } catch (Exception e) {
            log.error("사용자 저장 실패: {}", e.getMessage());
            throw new RuntimeException("사용자 저장에 실패했습니다.", e);
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        try {
            return userJpaRepository.findById(id)
                    .map(userJpaMapper::toDomain);
        } catch (Exception e) {
            log.error("사용자 조회 실패: id={}, error={}", id, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        try {
            return userJpaRepository.findByEmail(email)
                    .map(userJpaMapper::toDomain);
        } catch (Exception e) {
            log.error("이메일로 사용자 조회 실패: email={}, error={}", email, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        try {
            return userJpaRepository.findByUsername(username)
                    .map(userJpaMapper::toDomain);
        } catch (Exception e) {
            log.error("사용자명으로 사용자 조회 실패: username={}, error={}", username, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> findByApiKey(String apiKey) {
        try {
            return userJpaRepository.findByApiKey(apiKey)
                    .map(userJpaMapper::toDomain);
        } catch (Exception e) {
            log.error("API 키로 사용자 조회 실패: apiKey={}, error={}", apiKey, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<User> findAll() {
        try {
            return userJpaRepository.findAll().stream()
                    .map(userJpaMapper::toDomain)
                    .toList();
        } catch (Exception e) {
            log.error("전체 사용자 목록 조회 실패: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        try {
            return userJpaRepository.findAll(pageable)
                    .map(userJpaMapper::toDomain);
        } catch (Exception e) {
            log.error("페이징 사용자 목록 조회 실패: {}", e.getMessage());
            return Page.empty();
        }
    }

    @Override
    public void deleteById(Long id) {
        try {
            userJpaRepository.deleteById(id);
        } catch (Exception e) {
            log.error("사용자 삭제 실패: id={}, error={}", id, e.getMessage());
            throw new RuntimeException("사용자 삭제에 실패했습니다.", e);
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        try {
            return userJpaRepository.existsByEmail(email);
        } catch (Exception e) {
            log.error("이메일 존재 확인 실패: email={}, error={}", email, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        try {
            return userJpaRepository.existsByUsername(username);
        } catch (Exception e) {
            log.error("사용자명 존재 확인 실패: username={}, error={}", username, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean existsByApiKey(String apiKey) {
        try {
            return userJpaRepository.existsByApiKey(apiKey);
        } catch (Exception e) {
            log.error("API 키 존재 확인 실패: apiKey={}, error={}", apiKey, e.getMessage());
            return false;
        }
    }

    @Override
    public List<User> findLockedUsersBeforeUnlockTime(LocalDateTime unlockTime) {
        try {
            return userJpaRepository.findLockedUsersBeforeUnlockTime(unlockTime)
                    .stream()
                    .map(userJpaMapper::toDomain)
                    .toList();
        } catch (Exception e) {
            log.error("잠금 해제 대상 사용자 조회 실패: unlockTime={}, error={}", unlockTime, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<User> findPendingUsersAfter(UserStatus status, LocalDateTime after) {
        try {
            return userJpaRepository.findPendingUsersAfter(status, after)
                    .stream()
                    .map(userJpaMapper::toDomain)
                    .toList();
        } catch (Exception e) {
            log.error("승인 대기 사용자 조회 실패: status={}, after={}, error={}", status, after, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<User> findUsersWithPasswordChangedBefore(LocalDateTime before) {
        try {
            return userJpaRepository.findUsersWithPasswordChangedBefore(before)
                    .stream()
                    .map(userJpaMapper::toDomain)
                    .toList();
        } catch (Exception e) {
            log.error("비밀번호 만료 대상 사용자 조회 실패: before={}, error={}", before, e.getMessage());
            return List.of();
        }
    }

    @Override
    public long countPasswordChangesAfter(LocalDateTime after) {
        try {
            return userJpaRepository.countPasswordChangesAfter(after);
        } catch (Exception e) {
            log.error("비밀번호 변경 횟수 조회 실패: after={}, error={}", after, e.getMessage());
            return 0;
        }
    }

    @Override
    public long countUsersWithPasswordChangedBefore(LocalDateTime before) {
        try {
            return userJpaRepository.countUsersWithPasswordChangedBefore(before);
        } catch (Exception e) {
            log.error("비밀번호 만료 사용자 수 조회 실패: before={}, error={}", before, e.getMessage());
            return 0;
        }
    }

    @Override
    public Page<User> findByStatus(UserStatus status, Pageable pageable) {
        try {
            return userJpaRepository.findByStatus(status, pageable)
                    .map(userJpaMapper::toDomain);
        } catch (Exception e) {
            log.error("상태별 사용자 조회 실패: status={}, error={}", status, e.getMessage());
            return Page.empty();
        }
    }

    @Override
    public Page<User> findByStatusAndRole(UserStatus status, UserRole role, Pageable pageable) {
        try {
            return userJpaRepository.findByStatusAndRole(status, role, pageable)
                    .map(userJpaMapper::toDomain);
        } catch (Exception e) {
            log.error("상태 및 역할별 사용자 조회 실패: status={}, role={}, error={}", status, role, e.getMessage());
            return Page.empty();
        }
    }

    @Override
    public long countByStatus(UserStatus status) {
        try {
            return userJpaRepository.countByStatus(status);
        } catch (Exception e) {
            log.error("상태별 사용자 수 조회 실패: status={}, error={}", status, e.getMessage());
            return 0;
        }
    }

    @Override
    public List<User> findByWatchlistCategories(List<TargetCategory> categories) {
        try {
            List<UserJpaEntity> entities = userJpaRepository.findByWatchlistIn(categories);
            return entities.stream()
                    .map(userJpaMapper::toDomain)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("관심사별 사용자 조회 실패: categories={}, error={}", categories, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<User> findAllActiveUsers() {
        try {
            return userJpaRepository.findAllActiveUsers().stream()
                    .map(userJpaMapper::toDomain)
                    .toList();
        } catch (Exception e) {
            log.error("활성 사용자 목록 조회 실패: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public Optional<User> findByEmailAndUsername(String email, String username) {
        return userJpaRepository.findByEmailAndUsername(email, username)
                .map(userJpaMapper::toDomain);
    }

}