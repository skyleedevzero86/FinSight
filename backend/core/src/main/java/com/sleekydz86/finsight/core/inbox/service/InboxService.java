package com.sleekydz86.finsight.core.inbox.service;

import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.inbox.adapter.persistence.InboxNotificationJpaEntity;
import com.sleekydz86.finsight.core.inbox.adapter.persistence.InboxNotificationJpaRepository;
import com.sleekydz86.finsight.core.inbox.adapter.persistence.InboxSettingsJpaEntity;
import com.sleekydz86.finsight.core.inbox.adapter.persistence.InboxSettingsJpaRepository;
import com.sleekydz86.finsight.core.inbox.domain.InboxCategory;
import com.sleekydz86.finsight.core.inbox.domain.port.in.dto.InboxBroadcastRequest;
import com.sleekydz86.finsight.core.inbox.domain.port.in.dto.InboxBroadcastResponse;
import com.sleekydz86.finsight.core.inbox.domain.port.in.dto.InboxItemResponse;
import com.sleekydz86.finsight.core.inbox.domain.port.in.dto.InboxSettingsResponse;
import com.sleekydz86.finsight.core.inbox.domain.port.in.dto.InboxSettingsUpdateRequest;
import com.sleekydz86.finsight.core.inbox.domain.port.in.dto.InboxUnreadCountResponse;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.UserRole;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class InboxService {

    private final InboxNotificationJpaRepository inboxNotificationJpaRepository;
    private final InboxSettingsJpaRepository inboxSettingsJpaRepository;
    private final UserPersistencePort userPersistencePort;

    @Transactional(readOnly = true)
    public PaginationResponse<InboxItemResponse> list(Long userId, boolean unreadOnly, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)));
        Page<InboxNotificationJpaEntity> result = unreadOnly
                ? inboxNotificationJpaRepository
                        .findByRecipientUserIdAndDeletedFalseAndReadFalseOrderByCreatedAtDescIdDesc(userId, pageable)
                : inboxNotificationJpaRepository
                        .findByRecipientUserIdAndDeletedFalseOrderByCreatedAtDescIdDesc(userId, pageable);
        return PaginationResponse.from(result.map(this::toItem));
    }

    @Transactional(readOnly = true)
    public InboxUnreadCountResponse unreadCount(Long userId) {
        long count = inboxNotificationJpaRepository.countByRecipientUserIdAndDeletedFalseAndReadFalse(userId);
        return new InboxUnreadCountResponse(count);
    }

    @Transactional
    public InboxItemResponse markRead(Long userId, Long notificationId) {
        InboxNotificationJpaEntity entity = inboxNotificationJpaRepository
                .findByIdAndRecipientUserIdAndDeletedFalse(notificationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));
        if (!entity.isRead()) {
            entity.markRead();
        }
        return toItem(entity);
    }

    @Transactional
    public int markAllRead(Long userId) {
        int updated = inboxNotificationJpaRepository.markAllRead(userId, LocalDateTime.now());
        log.info("알림 모두 읽음 처리 - userId={}, count={}", userId, updated);
        return updated;
    }

    @Transactional
    public void deleteOne(Long userId, Long notificationId) {
        InboxNotificationJpaEntity entity = inboxNotificationJpaRepository
                .findByIdAndRecipientUserIdAndDeletedFalse(notificationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));
        entity.softDelete();
    }

    @Transactional
    public int deleteAll(Long userId) {
        int updated = inboxNotificationJpaRepository.softDeleteAll(userId, LocalDateTime.now());
        log.info("알림 모두 삭제 - userId={}, count={}", userId, updated);
        return updated;
    }

    @Transactional(readOnly = true)
    public InboxSettingsResponse getSettings(Long userId) {
        InboxSettingsJpaEntity settings = inboxSettingsJpaRepository.findById(userId)
                .orElseGet(() -> InboxSettingsJpaEntity.defaults(userId));
        return toSettings(settings);
    }

    @Transactional
    public InboxSettingsResponse updateSettings(Long userId, InboxSettingsUpdateRequest request) {
        InboxSettingsJpaEntity settings = inboxSettingsJpaRepository.findById(userId)
                .orElseGet(() -> InboxSettingsJpaEntity.defaults(userId));
        settings.update(
                Boolean.TRUE.equals(request.youtubeEnabled()),
                Boolean.TRUE.equals(request.newsEnabled()),
                Boolean.TRUE.equals(request.commentEnabled()),
                Boolean.TRUE.equals(request.qnaEnabled()));
        inboxSettingsJpaRepository.save(settings);
        log.info("알림 수신 설정 변경 - userId={}", userId);
        return toSettings(settings);
    }

    @Transactional(readOnly = true)
    public PaginationResponse<InboxItemResponse> adminList(int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)));
        Page<InboxNotificationJpaEntity> result = inboxNotificationJpaRepository.findAllActive(pageable);
        return PaginationResponse.from(result.map(this::toItem));
    }

    @Transactional
    public InboxBroadcastResponse broadcast(InboxBroadcastRequest request) {
        List<User> targets = resolveBroadcastTargets(request);
        if (targets.isEmpty()) {
            return new InboxBroadcastResponse(0);
        }
        String actorName = blankToDefault(request.actorName(), "FinSight");
        LocalDateTime now = LocalDateTime.now();
        List<InboxNotificationJpaEntity> entities = new ArrayList<>(targets.size());
        for (User user : targets) {
            if (!isCategoryEnabled(user.getId(), request.category())) {
                continue;
            }
            entities.add(InboxNotificationJpaEntity.builder()
                    .recipientUserId(user.getId())
                    .category(request.category())
                    .actorName(actorName)
                    .actorAvatarUrl(request.actorAvatarUrl())
                    .title(request.title())
                    .body(request.body())
                    .linkUrl(request.linkUrl())
                    .refType("ADMIN_BROADCAST")
                    .read(false)
                    .deleted(false)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        }
        inboxNotificationJpaRepository.saveAll(entities);
        log.info("관리자 알림 일괄 등록 - category={}, created={}", request.category(), entities.size());
        return new InboxBroadcastResponse(entities.size());
    }

    @Transactional
    public void createForUser(
            Long recipientUserId,
            InboxCategory category,
            Long actorUserId,
            String actorName,
            String actorAvatarUrl,
            String title,
            String body,
            String linkUrl,
            String refType,
            Long refId) {
        if (recipientUserId == null || Objects.equals(recipientUserId, actorUserId)) {
            return;
        }
        if (!isCategoryEnabled(recipientUserId, category)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        inboxNotificationJpaRepository.save(InboxNotificationJpaEntity.builder()
                .recipientUserId(recipientUserId)
                .category(category)
                .actorUserId(actorUserId)
                .actorName(actorName)
                .actorAvatarUrl(actorAvatarUrl)
                .title(title)
                .body(body)
                .linkUrl(linkUrl)
                .refType(refType)
                .refId(refId)
                .read(false)
                .deleted(false)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    @Transactional
    public void createForUsers(
            List<User> recipients,
            InboxCategory category,
            Long actorUserId,
            String actorName,
            String actorAvatarUrl,
            String title,
            String body,
            String linkUrl,
            String refType,
            Long refId) {
        if (recipients == null || recipients.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<InboxNotificationJpaEntity> entities = new ArrayList<>();
        for (User user : recipients) {
            if (user == null || user.getId() == null) {
                continue;
            }
            if (Objects.equals(user.getId(), actorUserId)) {
                continue;
            }
            if (!isCategoryEnabled(user.getId(), category)) {
                continue;
            }
            entities.add(InboxNotificationJpaEntity.builder()
                    .recipientUserId(user.getId())
                    .category(category)
                    .actorUserId(actorUserId)
                    .actorName(actorName)
                    .actorAvatarUrl(actorAvatarUrl)
                    .title(title)
                    .body(body)
                    .linkUrl(linkUrl)
                    .refType(refType)
                    .refId(refId)
                    .read(false)
                    .deleted(false)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        }
        if (!entities.isEmpty()) {
            inboxNotificationJpaRepository.saveAll(entities);
        }
    }

    public boolean isCategoryEnabled(Long userId, InboxCategory category) {
        if (category == InboxCategory.ADMIN || category == InboxCategory.WATCHLIST) {
            if (category == InboxCategory.WATCHLIST) {
                InboxSettingsJpaEntity settings = inboxSettingsJpaRepository.findById(userId).orElse(null);
                return settings == null || settings.isNewsEnabled();
            }
            return true;
        }
        InboxSettingsJpaEntity settings = inboxSettingsJpaRepository.findById(userId).orElse(null);
        if (settings == null) {
            return true;
        }
        return switch (category) {
            case YOUTUBE -> settings.isYoutubeEnabled();
            case NEWS -> settings.isNewsEnabled();
            case COMMENT -> settings.isCommentEnabled();
            case QNA -> settings.isQnaEnabled();
            default -> true;
        };
    }

    private List<User> resolveBroadcastTargets(InboxBroadcastRequest request) {
        if (Boolean.TRUE.equals(request.adminsOnly())) {
            return userPersistencePort.findAllActiveUsers().stream()
                    .filter(u -> u.getRole() == UserRole.ADMIN || u.getRole() == UserRole.MANAGER)
                    .toList();
        }
        if (Boolean.TRUE.equals(request.allUsers()) || request.userIds() == null || request.userIds().isEmpty()) {
            return userPersistencePort.findAllActiveUsers();
        }
        List<User> users = new ArrayList<>();
        for (Long id : request.userIds()) {
            userPersistencePort.findById(id).ifPresent(users::add);
        }
        return users;
    }

    private InboxItemResponse toItem(InboxNotificationJpaEntity entity) {
        return new InboxItemResponse(
                entity.getId(),
                entity.getCategory(),
                entity.getActorUserId(),
                entity.getActorName(),
                entity.getActorAvatarUrl(),
                entity.getTitle(),
                entity.getBody(),
                entity.getLinkUrl(),
                entity.getRefType(),
                entity.getRefId(),
                entity.isRead(),
                entity.getCreatedAt());
    }

    private InboxSettingsResponse toSettings(InboxSettingsJpaEntity settings) {
        return new InboxSettingsResponse(
                settings.isYoutubeEnabled(),
                settings.isNewsEnabled(),
                settings.isCommentEnabled(),
                settings.isQnaEnabled());
    }

    private String blankToDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
