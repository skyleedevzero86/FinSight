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
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.UserRole;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboxServiceTest {

    @Mock
    private InboxNotificationJpaRepository notificationRepository;

    @Mock
    private InboxSettingsJpaRepository settingsRepository;

    @Mock
    private UserPersistencePort userPersistencePort;

    private InboxService inboxService;

    @BeforeEach
    void setUp() {
        inboxService = new InboxService(notificationRepository, settingsRepository, userPersistencePort);
    }

    @Test
    void listUsesUnreadQueryClampsPaginationAndMapsEveryResponseField() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 4, 12, 30);
        InboxNotificationJpaEntity entity = notification(
                7L, InboxCategory.COMMENT, 9L, "Writer", "New reply", false, createdAt);
        ReflectionTestUtils.setField(entity, "id", 42L);
        PageRequest expectedPage = PageRequest.of(0, 50);
        when(notificationRepository
                .findByRecipientUserIdAndDeletedFalseAndReadFalseOrderByCreatedAtDescIdDesc(7L, expectedPage))
                .thenReturn(new PageImpl<>(List.of(entity), expectedPage, 1));

        PaginationResponse<InboxItemResponse> result = inboxService.list(7L, true, -3, 500);

        assertThat(result.getPage()).isZero();
        assertThat(result.getSize()).isEqualTo(50);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(42L);
            assertThat(item.category()).isEqualTo(InboxCategory.COMMENT);
            assertThat(item.actorUserId()).isEqualTo(9L);
            assertThat(item.actorName()).isEqualTo("Writer");
            assertThat(item.actorAvatarUrl()).isEqualTo("/avatar.png");
            assertThat(item.title()).isEqualTo("New reply");
            assertThat(item.body()).isEqualTo("body");
            assertThat(item.linkUrl()).isEqualTo("/community/free/3");
            assertThat(item.refType()).isEqualTo("COMMENT");
            assertThat(item.refId()).isEqualTo(33L);
            assertThat(item.read()).isFalse();
            assertThat(item.createdAt()).isEqualTo(createdAt);
        });
        verify(notificationRepository, never())
                .findByRecipientUserIdAndDeletedFalseOrderByCreatedAtDescIdDesc(anyLong(), any(Pageable.class));
    }

    @Test
    void listUsesAllItemsQueryAndEnforcesMinimumPageSize() {
        PageRequest expectedPage = PageRequest.of(2, 1);
        when(notificationRepository.findByRecipientUserIdAndDeletedFalseOrderByCreatedAtDescIdDesc(7L, expectedPage))
                .thenReturn(new PageImpl<>(List.of(), expectedPage, 0));

        PaginationResponse<InboxItemResponse> result = inboxService.list(7L, false, 2, 0);

        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(1);
        verify(notificationRepository, never())
                .findByRecipientUserIdAndDeletedFalseAndReadFalseOrderByCreatedAtDescIdDesc(
                        anyLong(), any(Pageable.class));
    }

    @Test
    void unreadCountReturnsRepositoryCount() {
        when(notificationRepository.countByRecipientUserIdAndDeletedFalseAndReadFalse(7L)).thenReturn(12L);

        assertThat(inboxService.unreadCount(7L).unreadCount()).isEqualTo(12L);
    }

    @Test
    void markReadUpdatesUnreadNotificationAndIsIdempotentForReadNotification() {
        InboxNotificationJpaEntity unread = notification(
                7L, InboxCategory.NEWS, null, "FinSight", "News", false, LocalDateTime.now());
        InboxNotificationJpaEntity alreadyRead = notification(
                7L, InboxCategory.NEWS, null, "FinSight", "News", true, LocalDateTime.now());
        LocalDateTime originalReadAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        ReflectionTestUtils.setField(alreadyRead, "readAt", originalReadAt);
        when(notificationRepository.findByIdAndRecipientUserIdAndDeletedFalse(1L, 7L))
                .thenReturn(Optional.of(unread));
        when(notificationRepository.findByIdAndRecipientUserIdAndDeletedFalse(2L, 7L))
                .thenReturn(Optional.of(alreadyRead));

        assertThat(inboxService.markRead(7L, 1L).read()).isTrue();
        assertThat(unread.getReadAt()).isNotNull();
        assertThat(inboxService.markRead(7L, 2L).read()).isTrue();
        assertThat(alreadyRead.getReadAt()).isEqualTo(originalReadAt);
    }

    @Test
    void markReadRejectsMissingOrOtherUsersNotification() {
        when(notificationRepository.findByIdAndRecipientUserIdAndDeletedFalse(99L, 7L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> inboxService.markRead(7L, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("알림을 찾을 수 없습니다.");
    }

    @Test
    void bulkOperationsReturnAffectedCounts() {
        when(notificationRepository.markAllRead(org.mockito.ArgumentMatchers.eq(7L), any(LocalDateTime.class)))
                .thenReturn(4);
        when(notificationRepository.softDeleteAll(org.mockito.ArgumentMatchers.eq(7L), any(LocalDateTime.class)))
                .thenReturn(6);

        assertThat(inboxService.markAllRead(7L)).isEqualTo(4);
        assertThat(inboxService.deleteAll(7L)).isEqualTo(6);
    }

    @Test
    void deleteOneSoftDeletesOwnedNotificationAndRejectsMissingNotification() {
        InboxNotificationJpaEntity entity = notification(
                7L, InboxCategory.ADMIN, null, "FinSight", "Notice", false, LocalDateTime.now());
        when(notificationRepository.findByIdAndRecipientUserIdAndDeletedFalse(1L, 7L))
                .thenReturn(Optional.of(entity));
        when(notificationRepository.findByIdAndRecipientUserIdAndDeletedFalse(2L, 7L))
                .thenReturn(Optional.empty());

        inboxService.deleteOne(7L, 1L);

        assertThat(entity.isDeleted()).isTrue();
        assertThatThrownBy(() -> inboxService.deleteOne(7L, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("알림을 찾을 수 없습니다.");
    }

    @Test
    void settingsDefaultToEnabledWithoutPersistingDuringRead() {
        when(settingsRepository.findById(7L)).thenReturn(Optional.empty());

        InboxSettingsResponse result = inboxService.getSettings(7L);

        assertThat(result.youtubeEnabled()).isTrue();
        assertThat(result.newsEnabled()).isTrue();
        assertThat(result.commentEnabled()).isTrue();
        assertThat(result.qnaEnabled()).isTrue();
        verify(settingsRepository, never()).save(any());
    }

    @Test
    void updateSettingsCreatesDefaultsThenAppliesEveryRequestedValue() {
        when(settingsRepository.findById(7L)).thenReturn(Optional.empty());
        InboxSettingsUpdateRequest request = new InboxSettingsUpdateRequest(true, false, null, true);

        InboxSettingsResponse result = inboxService.updateSettings(7L, request);

        ArgumentCaptor<InboxSettingsJpaEntity> captor = ArgumentCaptor.forClass(InboxSettingsJpaEntity.class);
        verify(settingsRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
        assertThat(result.youtubeEnabled()).isTrue();
        assertThat(result.newsEnabled()).isFalse();
        assertThat(result.commentEnabled()).isFalse();
        assertThat(result.qnaEnabled()).isTrue();
    }

    @Test
    void broadcastToAdminsFiltersRolesHonorsPreferencesAndNormalizesActorName() {
        User admin = user(1L, UserRole.ADMIN);
        User manager = user(2L, UserRole.MANAGER);
        User regularUser = user(3L, UserRole.USER);
        when(userPersistencePort.findAllActiveUsers()).thenReturn(List.of(admin, manager, regularUser));
        when(settingsRepository.findById(1L)).thenReturn(Optional.empty());
        when(settingsRepository.findById(2L)).thenReturn(Optional.of(settings(2L, true, false, true, true)));
        InboxBroadcastRequest request = new InboxBroadcastRequest(
                InboxCategory.NEWS, "Market update", "body", "/news", "  ", "/logo.png",
                false, true, List.of(3L));

        InboxBroadcastResponse result = inboxService.broadcast(request);

        assertThat(result.createdCount()).isEqualTo(1);
        ArgumentCaptor<List<InboxNotificationJpaEntity>> captor = listCaptor();
        verify(notificationRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(entity -> {
            assertThat(entity.getRecipientUserId()).isEqualTo(1L);
            assertThat(entity.getCategory()).isEqualTo(InboxCategory.NEWS);
            assertThat(entity.getActorName()).isEqualTo("FinSight");
            assertThat(entity.getTitle()).isEqualTo("Market update");
            assertThat(entity.getRefType()).isEqualTo("ADMIN_BROADCAST");
            assertThat(entity.isRead()).isFalse();
            assertThat(entity.isDeleted()).isFalse();
        });
    }

    @Test
    void broadcastTargetsExplicitUsersAndDoesNotSaveWhenNoUserResolves() {
        User first = user(1L, UserRole.USER);
        when(userPersistencePort.findById(1L)).thenReturn(Optional.of(first));
        when(userPersistencePort.findById(999L)).thenReturn(Optional.empty());
        InboxBroadcastRequest request = new InboxBroadcastRequest(
                InboxCategory.ADMIN, "Maintenance", null, null, "  Operator  ", null,
                false, false, List.of(1L, 999L));

        assertThat(inboxService.broadcast(request).createdCount()).isEqualTo(1);
        ArgumentCaptor<List<InboxNotificationJpaEntity>> captor = listCaptor();
        verify(notificationRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement()
                .extracting(InboxNotificationJpaEntity::getActorName)
                .isEqualTo("Operator");

        InboxBroadcastRequest emptyRequest = new InboxBroadcastRequest(
                InboxCategory.ADMIN, "Maintenance", null, null, null, null,
                false, false, List.of(888L));
        when(userPersistencePort.findById(888L)).thenReturn(Optional.empty());
        assertThat(inboxService.broadcast(emptyRequest).createdCount()).isZero();
        verify(notificationRepository).saveAll(captor.capture());
    }

    @Test
    void createForUserSkipsInvalidSelfAndDisabledRecipients() {
        inboxService.createForUser(null, InboxCategory.COMMENT, 3L, "Actor", null,
                "title", null, "/link", "COMMENT", 10L);
        inboxService.createForUser(3L, InboxCategory.COMMENT, 3L, "Actor", null,
                "title", null, "/link", "COMMENT", 10L);
        when(settingsRepository.findById(7L)).thenReturn(Optional.of(settings(7L, true, true, false, true)));
        inboxService.createForUser(7L, InboxCategory.COMMENT, 3L, "Actor", null,
                "title", null, "/link", "COMMENT", 10L);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createForUserPersistsAllNotificationDataWhenEnabled() {
        when(settingsRepository.findById(7L)).thenReturn(Optional.empty());

        inboxService.createForUser(7L, InboxCategory.QNA, 3L, "Actor", "/actor.png",
                "title", "body", "/community/qna/5", "COMMENT", 10L);

        ArgumentCaptor<InboxNotificationJpaEntity> captor = ArgumentCaptor.forClass(InboxNotificationJpaEntity.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue()).satisfies(entity -> {
            assertThat(entity.getRecipientUserId()).isEqualTo(7L);
            assertThat(entity.getCategory()).isEqualTo(InboxCategory.QNA);
            assertThat(entity.getActorUserId()).isEqualTo(3L);
            assertThat(entity.getActorName()).isEqualTo("Actor");
            assertThat(entity.getActorAvatarUrl()).isEqualTo("/actor.png");
            assertThat(entity.getTitle()).isEqualTo("title");
            assertThat(entity.getBody()).isEqualTo("body");
            assertThat(entity.getLinkUrl()).isEqualTo("/community/qna/5");
            assertThat(entity.getRefType()).isEqualTo("COMMENT");
            assertThat(entity.getRefId()).isEqualTo(10L);
            assertThat(entity.getCreatedAt()).isNotNull();
            assertThat(entity.getUpdatedAt()).isNotNull();
        });
    }

    @Test
    void createForUsersFiltersNullMissingSelfAndOptedOutRecipients() {
        User missingId = user(null, UserRole.USER);
        User actor = user(3L, UserRole.USER);
        User optedOut = user(4L, UserRole.USER);
        User eligible = user(5L, UserRole.USER);
        when(settingsRepository.findById(4L)).thenReturn(Optional.of(settings(4L, true, true, false, true)));
        when(settingsRepository.findById(5L)).thenReturn(Optional.empty());

        inboxService.createForUsers(List.of(missingId, actor, optedOut, eligible), InboxCategory.COMMENT,
                3L, "Actor", null, "title", null, "/link", "COMMENT", 10L);

        ArgumentCaptor<List<InboxNotificationJpaEntity>> captor = listCaptor();
        verify(notificationRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement()
                .extracting(InboxNotificationJpaEntity::getRecipientUserId)
                .isEqualTo(5L);
    }

    @Test
    void createForUsersReturnsEarlyForNullAndEmptyCollections() {
        inboxService.createForUsers(null, InboxCategory.ADMIN, null, "FinSight", null,
                "title", null, null, "ADMIN", null);
        inboxService.createForUsers(List.of(), InboxCategory.ADMIN, null, "FinSight", null,
                "title", null, null, "ADMIN", null);

        verifyNoInteractions(notificationRepository, settingsRepository, userPersistencePort);
    }

    @Test
    void categoryPreferencesUseDefaultsAndWatchlistSharesNewsPreference() {
        InboxSettingsJpaEntity settings = settings(7L, false, false, true, false);
        when(settingsRepository.findById(7L)).thenReturn(Optional.of(settings));
        when(settingsRepository.findById(8L)).thenReturn(Optional.empty());

        assertThat(inboxService.isCategoryEnabled(7L, InboxCategory.YOUTUBE)).isFalse();
        assertThat(inboxService.isCategoryEnabled(7L, InboxCategory.NEWS)).isFalse();
        assertThat(inboxService.isCategoryEnabled(7L, InboxCategory.WATCHLIST)).isFalse();
        assertThat(inboxService.isCategoryEnabled(7L, InboxCategory.COMMENT)).isTrue();
        assertThat(inboxService.isCategoryEnabled(7L, InboxCategory.QNA)).isFalse();
        assertThat(inboxService.isCategoryEnabled(7L, InboxCategory.ADMIN)).isTrue();
        assertThat(inboxService.isCategoryEnabled(8L, InboxCategory.QNA)).isTrue();
    }

    private InboxNotificationJpaEntity notification(
            Long recipientId,
            InboxCategory category,
            Long actorId,
            String actorName,
            String title,
            boolean read,
            LocalDateTime createdAt) {
        return InboxNotificationJpaEntity.builder()
                .recipientUserId(recipientId)
                .category(category)
                .actorUserId(actorId)
                .actorName(actorName)
                .actorAvatarUrl("/avatar.png")
                .title(title)
                .body("body")
                .linkUrl("/community/free/3")
                .refType("COMMENT")
                .refId(33L)
                .read(read)
                .deleted(false)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }

    private InboxSettingsJpaEntity settings(
            Long userId, boolean youtube, boolean news, boolean comment, boolean qna) {
        return InboxSettingsJpaEntity.builder()
                .userId(userId)
                .youtubeEnabled(youtube)
                .newsEnabled(news)
                .commentEnabled(comment)
                .qnaEnabled(qna)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private User user(Long id, UserRole role) {
        User user = User.builder().role(role).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<List<InboxNotificationJpaEntity>> listCaptor() {
        return ArgumentCaptor.forClass((Class) List.class);
    }
}
