package com.sleekydz86.finsight.core.comment.adapter.persistence.command;

import com.sleekydz86.finsight.core.comment.domain.Comment;
import com.sleekydz86.finsight.core.comment.domain.CommentStatus;
import com.sleekydz86.finsight.core.comment.domain.CommentType;
import com.sleekydz86.finsight.core.comment.domain.Comments;
import com.sleekydz86.finsight.core.comment.domain.port.out.CommentPersistencePort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CommentRepositoryImpl implements CommentPersistencePort {

    private static final List<CommentStatus> VISIBLE_STATUSES = List.of(
            CommentStatus.ACTIVE,
            CommentStatus.HIDDEN,
            CommentStatus.BLOCKED,
            CommentStatus.REPORTED);

    private final CommentJpaRepository commentJpaRepository;
    private final CommentJpaMapper commentJpaMapper;

    public CommentRepositoryImpl(CommentJpaRepository commentJpaRepository, CommentJpaMapper commentJpaMapper) {
        this.commentJpaRepository = commentJpaRepository;
        this.commentJpaMapper = commentJpaMapper;
    }

    @Override
    public Comment save(Comment comment) {
        CommentJpaEntity entity = commentJpaMapper.toEntity(comment);
        CommentJpaEntity savedEntity = commentJpaRepository.save(entity);
        return commentJpaMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Comment> findById(Long commentId) {
        return commentJpaRepository.findById(commentId)
                .map(commentJpaMapper::toDomain);
    }

    @Override
    public Comments findByTargetIdAndType(Long targetId, CommentType commentType) {
        List<CommentJpaEntity> entities = commentJpaRepository.findVisibleRoots(
                targetId, commentType, VISIBLE_STATUSES);
        return commentJpaMapper.toDomainList(entities);
    }

    @Override
    public Comments findByTargetIdAndTypeWithPagination(Long targetId, CommentType commentType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CommentJpaEntity> entityPage = commentJpaRepository.findVisibleRoots(
                targetId, commentType, VISIBLE_STATUSES, pageable);
        return commentJpaMapper.toDomainList(entityPage.getContent());
    }

    @Override
    public Comments findByUserEmail(String userEmail) {
        List<CommentJpaEntity> entities = commentJpaRepository
                .findByAuthorEmailAndStatusOrderByCreatedAtDesc(userEmail, CommentStatus.ACTIVE);
        return commentJpaMapper.toDomainList(entities);
    }

    @Override
    public Comments findByUserEmail(String userEmail, int page, int size) {
        Page<CommentJpaEntity> entityPage = commentJpaRepository
                .findByAuthorEmailAndStatusOrderByCreatedAtDesc(
                        userEmail, CommentStatus.ACTIVE, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        Comments comments = commentJpaMapper.toDomainList(entityPage.getContent());
        return new Comments(comments.getComments(), entityPage.getTotalElements());
    }

    @Override
    public Comments findReportedComments() {
        List<CommentJpaEntity> entities = commentJpaRepository
                .findReportedComments(CommentStatus.ACTIVE);
        return commentJpaMapper.toDomainList(entities);
    }

    @Override
    public List<Comment> findRepliesByParentId(Long parentId) {
        List<CommentJpaEntity> entities = commentJpaRepository.findVisibleReplies(parentId, VISIBLE_STATUSES);
        return entities.stream()
                .map(commentJpaMapper::toDomain)
                .toList();
    }

    @Override
    public List<Comment> findModerationCandidates(int minReportCount) {
        return commentJpaRepository.findModerationCandidates(CommentStatus.ACTIVE, minReportCount).stream()
                .map(commentJpaMapper::toDomain)
                .toList();
    }

    @Override
    public List<Comment> findByStatus(CommentStatus status) {
        return commentJpaRepository.findByStatusOrderByUpdatedAtDesc(status).stream()
                .map(commentJpaMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long commentId) {
        commentJpaRepository.deleteById(commentId);
    }

    @Override
    public long countByTargetIdAndType(Long targetId, CommentType commentType) {
        return commentJpaRepository.countByTargetIdAndCommentTypeAndStatus(
                targetId, commentType, CommentStatus.ACTIVE);
    }

    @Override
    public long countByAuthorEmail(String authorEmail) {
        return commentJpaRepository.countByAuthorEmailAndStatus(authorEmail, CommentStatus.ACTIVE);
    }

    @Override
    public long countByAuthorEmailBetween(String authorEmail, java.time.LocalDateTime from, java.time.LocalDateTime to) {
        return commentJpaRepository.countByAuthorEmailAndStatusAndCreatedAtBetween(
                authorEmail, CommentStatus.ACTIVE, from, to);
    }
}
