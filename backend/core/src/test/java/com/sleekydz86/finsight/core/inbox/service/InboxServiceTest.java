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
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("안 읽은 목록은 페이지를 제한하고 응답 필드를 모두 채운다")
    void listUsesUnreadQueryClampsPaginationAndMapsEveryResponseField() {
        // given
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 4, 12, 30);
        InboxNotificationJpaEntity entity = notification(
                7L, InboxCategory.COMMENT, 9L, "Writer", "New reply", false, createdAt);

        // when
        ReflectionTestUtils.setField(entity, "id", 42L);

        // given
        PageRequest expectedPage = PageRequest.of(0, 50);
        when(notificationRepository
                .findByRecipientUserIdAndDeletedFalseAndReadFalseOrderByCreatedAtDescIdDesc(7L, expectedPage))
                .thenReturn(new PageImpl<>(List.of(entity), expectedPage, 1));

        // when
        PaginationResponse<InboxItemResponse> result = inboxService.list(7L, true, -3, 500);

        // then
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
    @DisplayName("전체 목록은 최소 페이지 크기를 지킨다")
    void listUsesAllItemsQueryAndEnforcesMinimumPageSize() {
        // given
        PageRequest expectedPage = PageRequest.of(2, 1);
        when(notificationRepository.findByRecipientUserIdAndDeletedFalseOrderByCreatedAtDescIdDesc(7L, expectedPage))
                .thenReturn(new PageImpl<>(List.of(), expectedPage, 0));

        // when
        PaginationResponse<InboxItemResponse> result = inboxService.list(7L, false, 2, 0);

        // then
        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(1);
        verify(notificationRepository, never())
                .findByRecipientUserIdAndDeletedFalseAndReadFalseOrderByCreatedAtDescIdDesc(
                        anyLong(), any(Pageable.class));
    }

    @Test
    @DisplayName("안 읽은 수는 저장소 건수를 반환한다")
    void unreadCountReturnsRepositoryCount() {
        // given
        when(notificationRepository.countByRecipientUserIdAndDeletedFalseAndReadFalse(7L)).thenReturn(12L);

        // when

        // then
        assertThat(inboxService.unreadCount(7L).unreadCount()).isEqualTo(12L);
    }

    @Test
    @DisplayName("안 읽은 알림을 읽음으로 바꾸고 이미 읽은 알림은 그대로 둔다")
    void markReadUpdatesUnreadNotificationAndIsIdempotentForReadNotification() {
        // given
        InboxNotificationJpaEntity unread = notification(
                7L, InboxCategory.NEWS, null, "FinSight", "News", false, LocalDateTime.now());
        InboxNotificationJpaEntity alreadyRead = notification(
                7L, InboxCategory.NEWS, null, "FinSight", "News", true, LocalDateTime.now());
        LocalDateTime originalReadAt = LocalDateTime.of(2026, 1, 1, 0, 0);

        // when
        ReflectionTestUtils.setField(alreadyRead, "readAt", originalReadAt);

        // given
        when(notificationRepository.findByIdAndRecipientUserIdAndDeletedFalse(1L, 7L))
                .thenReturn(Optional.of(unread));
        when(notificationRepository.findByIdAndRecipientUserIdAndDeletedFalse(2L, 7L))
                .thenReturn(Optional.of(alreadyRead));

        // then
        assertThat(inboxService.markRead(7L, 1L).read()).isTrue();
        assertThat(unread.getReadAt()).isNotNull();
        assertThat(inboxService.markRead(7L, 2L).read()).isTrue();
        assertThat(alreadyRead.getReadAt()).isEqualTo(originalReadAt);
    }

    @Test
    @DisplayName("없거나 다른 사용자의 알림은 읽음 처리를 거절한다")
    void markReadRejectsMissingOrOtherUsersNotification() {
        // given
        when(notificationRepository.findByIdAndRecipientUserIdAndDeletedFalse(99L, 7L))
                .thenReturn(Optional.empty());

        // when

        // then
        assertThatThrownBy(() -> inboxService.markRead(7L, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("알림을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("일괄 작업은 반영 건수를 반환한다")
    void bulkOperationsReturnAffectedCounts() {
        // given
        when(notificationRepository.markAllRead(org.mockito.ArgumentMatchers.eq(7L), any(LocalDateTime.class)))
                .thenReturn(4);
        when(notificationRepository.softDeleteAll(org.mockito.ArgumentMatchers.eq(7L), any(LocalDateTime.class)))
                .thenReturn(6);

        // when

        // then
        assertThat(inboxService.markAllRead(7L)).isEqualTo(4);
        assertThat(inboxService.deleteAll(7L)).isEqualTo(6);
    }

    @Test
    @DisplayName("내 알림은 숨기고 없는 알림 삭제는 거절한다")
    void deleteOneSoftDeletesOwnedNotificationAndRejectsMissingNotification() {
        // given
        InboxNotificationJpaEntity entity = notification(
                7L, InboxCategory.ADMIN, null, "FinSight", "Notice", false, LocalDateTime.now());
        when(notificationRepository.findByIdAndRecipientUserIdAndDeletedFalse(1L, 7L))
                .thenReturn(Optional.of(entity));
        when(notificationRepository.findByIdAndRecipientUserIdAndDeletedFalse(2L, 7L))
                .thenReturn(Optional.empty());

        // when
        inboxService.deleteOne(7L, 1L);

        // then
        assertThat(entity.isDeleted()).isTrue();
        assertThatThrownBy(() -> inboxService.deleteOne(7L, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("알림을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("설정 조회는 저장하지 않고 기본값을 켠 상태로 본다")
    void settingsDefaultToEnabledWithoutPersistingDuringRead() {
        // given
        when(settingsRepository.findById(7L)).thenReturn(Optional.empty());

        // when
        InboxSettingsResponse result = inboxService.getSettings(7L);

        // then
        assertThat(result.youtubeEnabled()).isTrue();
        assertThat(result.newsEnabled()).isTrue();
        assertThat(result.commentEnabled()).isTrue();
        assertThat(result.qnaEnabled()).isTrue();
        verify(settingsRepository, never()).save(any());
    }

    @Test
    @DisplayName("설정이 없으면 기본값을 만든 뒤 요청 값을 반영한다")
    void updateSettingsCreatesDefaultsThenAppliesEveryRequestedValue() {
        // given
        when(settingsRepository.findById(7L)).thenReturn(Optional.empty());
        InboxSettingsUpdateRequest request = new InboxSettingsUpdateRequest(true, false, null, true);

        // when
        InboxSettingsResponse result = inboxService.updateSettings(7L, request);

        // given
        ArgumentCaptor<InboxSettingsJpaEntity> captor = ArgumentCaptor.forClass(InboxSettingsJpaEntity.class);

        // then
        verify(settingsRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
        assertThat(result.youtubeEnabled()).isTrue();
        assertThat(result.newsEnabled()).isFalse();
        assertThat(result.commentEnabled()).isFalse();
        assertThat(result.qnaEnabled()).isTrue();
    }

    @Test
    @DisplayName("관리자 방송은 권한과 설정을 보고 작성자 이름을 정리한다")
    void broadcastToAdminsFiltersRolesHonorsPreferencesAndNormalizesActorName() {
        // given
        User admin = user(1L, UserRole.ADMIN);
        User manager = user(2L, UserRole.MANAGER);
        User regularUser = user(3L, UserRole.USER);
        when(userPersistencePort.findAllActiveUsers()).thenReturn(List.of(admin, manager, regularUser));
        when(settingsRepository.findById(1L)).thenReturn(Optional.empty());
        when(settingsRepository.findById(2L)).thenReturn(Optional.of(settings(2L, true, false, true, true)));
        InboxBroadcastRequest request = new InboxBroadcastRequest(
                InboxCategory.NEWS, "Market update", "body", "/news", "  ", "/logo.png",
                false, true, List.of(3L));

        // when
        InboxBroadcastResponse result = inboxService.broadcast(request);

        // then
        assertThat(result.createdCount()).isEqualTo(1);

        // given
        ArgumentCaptor<List<InboxNotificationJpaEntity>> captor = listCaptor();

        // then
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
    @DisplayName("지정 사용자에게만 보내고 대상이 없으면 저장하지 않는다")
    void broadcastTargetsExplicitUsersAndDoesNotSaveWhenNoUserResolves() {
        // given
        User first = user(1L, UserRole.USER);
        when(userPersistencePort.findById(1L)).thenReturn(Optional.of(first));
        when(userPersistencePort.findById(999L)).thenReturn(Optional.empty());
        InboxBroadcastRequest request = new InboxBroadcastRequest(
                InboxCategory.ADMIN, "Maintenance", null, null, "  Operator  ", null,
                false, false, List.of(1L, 999L));

        // when

        // then
        assertThat(inboxService.broadcast(request).createdCount()).isEqualTo(1);

        // given
        ArgumentCaptor<List<InboxNotificationJpaEntity>> captor = listCaptor();

        // then
        verify(notificationRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement()
                .extracting(InboxNotificationJpaEntity::getActorName)
                .isEqualTo("Operator");

        // given
        InboxBroadcastRequest emptyRequest = new InboxBroadcastRequest(
                InboxCategory.ADMIN, "Maintenance", null, null, null, null,
                false, false, List.of(888L));
        when(userPersistencePort.findById(888L)).thenReturn(Optional.empty());

        // then
        assertThat(inboxService.broadcast(emptyRequest).createdCount()).isZero();
        verify(notificationRepository).saveAll(captor.capture());
    }

    @Test
    @DisplayName("잘못된 대상, 본인, 끈 사용자는 알림을 만들지 않는다")
    void createForUserSkipsInvalidSelfAndDisabledRecipients() {
        // when
        inboxService.createForUser(null, InboxCategory.COMMENT, 3L, "Actor", null,
                "title", null, "/link", "COMMENT", 10L);
        inboxService.createForUser(3L, InboxCategory.COMMENT, 3L, "Actor", null,
                "title", null, "/link", "COMMENT", 10L);

        // given
        when(settingsRepository.findById(7L)).thenReturn(Optional.of(settings(7L, true, true, false, true)));

        // when
        inboxService.createForUser(7L, InboxCategory.COMMENT, 3L, "Actor", null,
                "title", null, "/link", "COMMENT", 10L);

        // then
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("알림이 켜져 있으면 알림 데이터를 모두 저장한다")
    void createForUserPersistsAllNotificationDataWhenEnabled() {
        // given
        when(settingsRepository.findById(7L)).thenReturn(Optional.empty());

        // when
        inboxService.createForUser(7L, InboxCategory.QNA, 3L, "Actor", "/actor.png",
                "title", "body", "/community/qna/5", "COMMENT", 10L);

        // given
        ArgumentCaptor<InboxNotificationJpaEntity> captor = ArgumentCaptor.forClass(InboxNotificationJpaEntity.class);

        // then
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
    @DisplayName("없는 사용자, 본인, 끈 사용자는 여러 명 알림에서 뺀다")
    void createForUsersFiltersNullMissingSelfAndOptedOutRecipients() {
        // given
        User missingId = user(null, UserRole.USER);
        User actor = user(3L, UserRole.USER);
        User optedOut = user(4L, UserRole.USER);
        User eligible = user(5L, UserRole.USER);
        when(settingsRepository.findById(4L)).thenReturn(Optional.of(settings(4L, true, true, false, true)));
        when(settingsRepository.findById(5L)).thenReturn(Optional.empty());

        // when
        inboxService.createForUsers(List.of(missingId, actor, optedOut, eligible), InboxCategory.COMMENT,
                3L, "Actor", null, "title", null, "/link", "COMMENT", 10L);

        // given
        ArgumentCaptor<List<InboxNotificationJpaEntity>> captor = listCaptor();

        // then
        verify(notificationRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement()
                .extracting(InboxNotificationJpaEntity::getRecipientUserId)
                .isEqualTo(5L);
    }

    @Test
    @DisplayName("대상 목록이 없거나 비어 있으면 바로 끝낸다")
    void createForUsersReturnsEarlyForNullAndEmptyCollections() {
        // given

        // when
        inboxService.createForUsers(null, InboxCategory.ADMIN, null, "FinSight", null,
                "title", null, null, "ADMIN", null);
        inboxService.createForUsers(List.of(), InboxCategory.ADMIN, null, "FinSight", null,
                "title", null, null, "ADMIN", null);
        verifyNoInteractions(notificationRepository, settingsRepository, userPersistencePort);

        // then
    }

    @Test
    @DisplayName("카테고리 설정은 기본값을 쓰고 관심종목은 뉴스 설정을 따른다")
    void categoryPreferencesUseDefaultsAndWatchlistSharesNewsPreference() {
        // given
        InboxSettingsJpaEntity settings = settings(7L, false, false, true, false);
        when(settingsRepository.findById(7L)).thenReturn(Optional.of(settings));
        when(settingsRepository.findById(8L)).thenReturn(Optional.empty());

        // when

        // then
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
