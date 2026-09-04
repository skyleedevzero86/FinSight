package com.sleekydz86.finsight.core.inbox.adapter.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InboxNotificationJpaRepository extends JpaRepository<InboxNotificationJpaEntity, Long> {

    Page<InboxNotificationJpaEntity> findByRecipientUserIdAndDeletedFalseOrderByCreatedAtDescIdDesc(
            Long recipientUserId, Pageable pageable);

    Page<InboxNotificationJpaEntity> findByRecipientUserIdAndDeletedFalseAndReadFalseOrderByCreatedAtDescIdDesc(
            Long recipientUserId, Pageable pageable);

    long countByRecipientUserIdAndDeletedFalseAndReadFalse(Long recipientUserId);

    Optional<InboxNotificationJpaEntity> findByIdAndRecipientUserIdAndDeletedFalse(Long id, Long recipientUserId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE InboxNotificationJpaEntity n
            SET n.read = true, n.readAt = :readAt, n.updatedAt = :readAt
            WHERE n.recipientUserId = :userId AND n.deleted = false AND n.read = false
            """)
    int markAllRead(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE InboxNotificationJpaEntity n
            SET n.deleted = true, n.updatedAt = :updatedAt
            WHERE n.recipientUserId = :userId AND n.deleted = false
            """)
    int softDeleteAll(@Param("userId") Long userId, @Param("updatedAt") LocalDateTime updatedAt);

    @Query("""
            SELECT n FROM InboxNotificationJpaEntity n
            WHERE n.deleted = false
            ORDER BY n.createdAt DESC, n.id DESC
            """)
    Page<InboxNotificationJpaEntity> findAllActive(Pageable pageable);

    List<InboxNotificationJpaEntity> findByRecipientUserIdAndDeletedFalseAndIdIn(Long recipientUserId, List<Long> ids);
}
