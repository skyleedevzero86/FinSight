package com.sleekydz86.finsight.core.notification.adapter.persistence.command;

import com.sleekydz86.finsight.core.notification.domain.Notification;
import com.sleekydz86.finsight.core.notification.domain.NotificationStatus;
import com.sleekydz86.finsight.core.notification.domain.port.out.NotificationPersistencePort;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Transactional
public class NotificationRepositoryImpl implements NotificationPersistencePort {

    private final NotificationJpaRepository notificationJpaRepository;
    private final NotificationJpaMapper notificationJpaMapper;
    private final UserPersistencePort userPersistencePort;

    @Override
    public Notification save(Notification notification) {
        NotificationJpaEntity entity = notificationJpaMapper.toEntity(notification);
        NotificationJpaEntity savedEntity = notificationJpaRepository.save(entity);
        return notificationJpaMapper.toDomain(savedEntity);
    }

    @Override
    public List<Notification> saveAll(List<Notification> notifications) {
        List<NotificationJpaEntity> entities = notifications.stream()
                .map(notificationJpaMapper::toEntity)
                .collect(Collectors.toList());

        List<NotificationJpaEntity> savedEntities = notificationJpaRepository.saveAll(entities);
        return savedEntities.stream()
                .map(notificationJpaMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Notification findById(Long id) {
        return notificationJpaRepository.findById(id)
                .map(notificationJpaMapper::toDomain)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Notification> findByUser(User user, Pageable pageable) {
        Page<NotificationJpaEntity> entityPage = notificationJpaRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        return entityPage.map(notificationJpaMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findByStatus(NotificationStatus status) {
        return notificationJpaRepository.findByStatus(status).stream()
                .map(notificationJpaMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findByScheduledAtBefore(LocalDateTime dateTime) {
        return notificationJpaRepository
                .findByScheduledAtBeforeAndStatus(dateTime, NotificationStatus.PENDING).stream()
                .map(notificationJpaMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findByFailedStatus() {
        return notificationJpaRepository
                .findByStatusAndCreatedAtBefore(NotificationStatus.FAILED, LocalDateTime.now().minusDays(7)).stream()
                .map(notificationJpaMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long countByUser(User user) {
        return notificationJpaRepository.countByUserId(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findByDateRange(LocalDateTime start, LocalDateTime end) {
        return notificationJpaRepository.findByUserIdAndDateRange(0L, start, end).stream()
                .map(notificationJpaMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        notificationJpaRepository.deleteById(id);
    }
}