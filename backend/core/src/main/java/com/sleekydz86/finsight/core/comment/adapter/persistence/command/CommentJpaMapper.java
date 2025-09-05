package com.sleekydz86.finsight.core.comment.adapter.persistence.command;

import com.sleekydz86.finsight.core.comment.domain.Comment;
import com.sleekydz86.finsight.core.comment.domain.CommentStatus;
import com.sleekydz86.finsight.core.comment.domain.CommentType;
import com.sleekydz86.finsight.core.comment.domain.Comments;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CommentJpaMapper {

    public Comment toDomain(CommentJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return Comment.builder()
                .id(entity.getId())
                .content(entity.getContent())
                .authorEmail(entity.getAuthorEmail())
                .commentType(entity.getCommentType())
                .targetId(entity.getTargetId())
                .parentId(entity.getParentId())
                .status(entity.getStatus())
                .likeCount(entity.getLikeCount())
                .dislikeCount(entity.getDislikeCount())
                .reportCount(entity.getReportCount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .replies(List.of())
                .build();
    }

    public CommentJpaEntity toEntity(Comment comment) {
        if (comment == null) {
            return null;
        }

        CommentJpaEntity entity = new CommentJpaEntity();
        entity.setId(comment.getId());
        entity.setContent(comment.getContent());
        entity.setAuthorEmail(comment.getAuthorEmail());
        entity.setCommentType(comment.getCommentType());
        entity.setTargetId(comment.getTargetId());
        entity.setParentId(comment.getParentId());
        entity.setStatus(comment.getStatus());
        entity.setLikeCount(comment.getLikeCount());
        entity.setDislikeCount(comment.getDislikeCount());
        entity.setReportCount(comment.getReportCount());
        entity.setCreatedAt(comment.getCreatedAt());
        entity.setUpdatedAt(comment.getUpdatedAt());

        return entity;
    }

    public Comments toDomainList(List<CommentJpaEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return new Comments();
        }

        List<Comment> comments = entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());

        return new Comments(comments);
    }
}