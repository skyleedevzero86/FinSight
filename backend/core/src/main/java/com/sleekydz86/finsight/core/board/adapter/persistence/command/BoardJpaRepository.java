package com.sleekydz86.finsight.core.board.adapter.persistence.command;

import com.sleekydz86.finsight.core.board.domain.BoardStatus;
import com.sleekydz86.finsight.core.board.domain.BoardType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BoardJpaRepository extends JpaRepository<BoardJpaEntity, Long> {

    Page<BoardJpaEntity> findByBoardTypeAndStatusOrderByCreatedAtDesc(
            BoardType boardType, BoardStatus status, Pageable pageable);

    Page<BoardJpaEntity> findByAuthorEmailAndStatusOrderByCreatedAtDesc(
            String authorEmail, BoardStatus status, Pageable pageable);

    List<BoardJpaEntity> findByStatusOrderByReportCountDesc(BoardStatus status);

    @Query("SELECT b FROM BoardJpaEntity b WHERE b.status = :status AND b.reportCount > 0 ORDER BY b.reportCount DESC")
    List<BoardJpaEntity> findReportedBoards(@Param("status") BoardStatus status);

    @Query("SELECT b FROM BoardJpaEntity b WHERE b.boardType = :boardType AND b.status = :status ORDER BY b.viewCount DESC")
    List<BoardJpaEntity> findPopularBoards(@Param("boardType") BoardType boardType,
                                           @Param("status") BoardStatus status,
                                           Pageable pageable);

    @Query("SELECT b FROM BoardJpaEntity b WHERE b.boardType = :boardType AND b.status = :status ORDER BY b.createdAt DESC")
    List<BoardJpaEntity> findLatestBoards(@Param("boardType") BoardType boardType,
                                          @Param("status") BoardStatus status,
                                          Pageable pageable);

    @Query("SELECT b FROM BoardJpaEntity b WHERE b.boardType = :boardType AND b.status = :status " +
            "AND b.createdAt BETWEEN :startDate AND :endDate " +
            "ORDER BY b.createdAt DESC")
    Page<BoardJpaEntity> findByBoardTypeAndStatusAndCreatedAtBetween(
            @Param("boardType") BoardType boardType,
            @Param("status") BoardStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT b FROM BoardJpaEntity b WHERE b.boardType = :boardType AND b.status = :status " +
            "AND (b.title LIKE %:keyword% OR b.content LIKE %:keyword%) " +
            "ORDER BY b.createdAt DESC")
    Page<BoardJpaEntity> findByBoardTypeAndStatusAndTitleContainingOrContentContaining(
            @Param("boardType") BoardType boardType,
            @Param("status") BoardStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT b FROM BoardJpaEntity b WHERE b.boardType = :boardType AND b.status = :status " +
            "AND :hashtag MEMBER OF b.hashtags " +
            "ORDER BY b.createdAt DESC")
    Page<BoardJpaEntity> findByBoardTypeAndStatusAndHashtagsContaining(
            @Param("boardType") BoardType boardType,
            @Param("status") BoardStatus status,
            @Param("hashtag") String hashtag,
            Pageable pageable);

    @Query("SELECT b FROM BoardJpaEntity b WHERE b.boardType = :boardType AND b.status = :status " +
            "AND b.id < :boardId " +
            "ORDER BY b.id DESC")
    List<BoardJpaEntity> findPreviousBoard(@Param("boardType") BoardType boardType,
                                           @Param("status") BoardStatus status,
                                           @Param("boardId") Long boardId,
                                           Pageable pageable);

    @Query("SELECT b FROM BoardJpaEntity b WHERE b.boardType = :boardType AND b.status = :status " +
            "AND b.id > :boardId " +
            "ORDER BY b.id ASC")
    List<BoardJpaEntity> findNextBoard(@Param("boardType") BoardType boardType,
                                       @Param("status") BoardStatus status,
                                       @Param("boardId") Long boardId,
                                       Pageable pageable);

    long countByBoardTypeAndStatus(BoardType boardType, BoardStatus status);
    long countByAuthorEmailAndStatus(String authorEmail, BoardStatus status);

    @Modifying
    @Query("UPDATE BoardJpaEntity b SET b.viewCount = b.viewCount + 1 WHERE b.id = :boardId")
    void incrementViewCount(@Param("boardId") Long boardId);

    @Modifying
    @Query("UPDATE BoardJpaEntity b SET b.likeCount = b.likeCount + 1 WHERE b.id = :boardId")
    void incrementLikeCount(@Param("boardId") Long boardId);

    @Modifying
    @Query("UPDATE BoardJpaEntity b SET b.dislikeCount = b.dislikeCount + 1 WHERE b.id = :boardId")
    void incrementDislikeCount(@Param("boardId") Long boardId);

    @Modifying
    @Query("UPDATE BoardJpaEntity b SET b.commentCount = :commentCount WHERE b.id = :boardId")
    void updateCommentCount(@Param("boardId") Long boardId, @Param("commentCount") int commentCount);

    @Modifying
    @Query("UPDATE BoardJpaEntity b SET b.reportCount = b.reportCount + 1 WHERE b.id = :boardId")
    void incrementReportCount(@Param("boardId") Long boardId);
}