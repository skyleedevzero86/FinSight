package com.sleekydz86.finsight.core.comment.service;

import com.sleekydz86.finsight.core.comment.domain.Comment;
import com.sleekydz86.finsight.core.comment.domain.CommentReaction;
import com.sleekydz86.finsight.core.comment.domain.Comments;
import com.sleekydz86.finsight.core.comment.domain.CommentType;
import com.sleekydz86.finsight.core.comment.domain.port.in.CommentQueryUseCase;
import com.sleekydz86.finsight.core.comment.domain.port.in.dto.CommentResponse;
import com.sleekydz86.finsight.core.comment.domain.port.out.CommentPersistencePort;
import com.sleekydz86.finsight.core.comment.domain.port.out.CommentReactionPersistencePort;
import com.sleekydz86.finsight.core.comment.domain.port.out.CommentReportPersistencePort;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardPersistencePort;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.global.exception.NewsNotFoundException;
import com.sleekydz86.finsight.core.global.exception.SystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CommentQueryService implements CommentQueryUseCase {

    private static final Logger log = LoggerFactory.getLogger(CommentQueryService.class);

    private final CommentPersistencePort commentPersistencePort;
    private final CommentReactionPersistencePort commentReactionPersistencePort;
    private final CommentReportPersistencePort commentReportPersistencePort;
    private final BoardPersistencePort boardPersistencePort;

    public CommentQueryService(CommentPersistencePort commentPersistencePort,
                               CommentReactionPersistencePort commentReactionPersistencePort,
                               CommentReportPersistencePort commentReportPersistencePort,
                               BoardPersistencePort boardPersistencePort) {
        this.commentPersistencePort = commentPersistencePort;
        this.commentReactionPersistencePort = commentReactionPersistencePort;
        this.commentReportPersistencePort = commentReportPersistencePort;
        this.boardPersistencePort = boardPersistencePort;
    }

    @Override
    public Comments getCommentsByTargetId(Long targetId, CommentType commentType) {
        log.info("대상 댓글 조회 - 대상 ID: {}, 댓글 타입: {}", targetId, commentType);

        Comments comments = commentPersistencePort.findByTargetIdAndType(targetId, commentType);

        Comments commentsWithReplies = new Comments();
        for (Comment comment : comments.getComments()) {
            if (comment.getParentId() != null) {
                continue;
            }
            List<Comment> replies = commentPersistencePort.findRepliesByParentId(comment.getId());
            Comment commentWithReplies = comment;
            for (Comment reply : replies) {
                commentWithReplies = commentWithReplies.addReply(reply);
            }
            commentsWithReplies = commentsWithReplies.addComment(commentWithReplies);
        }

        return commentsWithReplies;
    }

    @Override
    public Comments getCommentsByTargetIdWithPagination(Long targetId, CommentType commentType, int page, int size) {
        log.info("대상 댓글 페이징 조회 - 대상 ID: {}, 댓글 타입: {}, 페이지: {}, 크기: {}",
                targetId, commentType, page, size);

        Comments comments = commentPersistencePort.findByTargetIdAndTypeWithPagination(targetId, commentType, page, size);

        Comments commentsWithReplies = new Comments();
        for (Comment comment : comments.getComments()) {
            if (comment.getParentId() != null) {
                continue;
            }
            List<Comment> replies = commentPersistencePort.findRepliesByParentId(comment.getId());
            Comment commentWithReplies = comment;
            for (Comment reply : replies) {
                commentWithReplies = commentWithReplies.addReply(reply);
            }
            commentsWithReplies = commentsWithReplies.addComment(commentWithReplies);
        }

        return commentsWithReplies;
    }

    @Override
    public Comment getCommentById(Long commentId) {
        log.info("댓글 단건 조회 - 댓글 ID: {}", commentId);

        return commentPersistencePort.findById(commentId)
                .orElseThrow(() -> new NewsNotFoundException(commentId));
    }

    @Override
    public List<CommentResponse> getCommentReplies(Long parentId) {
        log.info("대댓글 조회 - 부모 댓글 ID: {}", parentId);

        List<Comment> replies = commentPersistencePort.findRepliesByParentId(parentId);
        return replies.stream()
                .map(CommentResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public Comments getCommentsByUser(String userEmail) {
        log.info("사용자 댓글 조회 - 사용자 이메일: {}", userEmail);

        return commentPersistencePort.findByUserEmail(userEmail);
    }

    @Override
    public Comments getReportedComments() {
        log.info("신고된 댓글 조회");

        return commentPersistencePort.findReportedComments();
    }

    @Override
    public boolean hasUserLikedComment(String userEmail, Long commentId) {
        Optional<CommentReaction> reaction = commentReactionPersistencePort
                .findByCommentIdAndUserEmail(commentId, userEmail);
        return reaction.isPresent() && reaction.get().isLike();
    }

    @Override
    public boolean hasUserDislikedComment(String userEmail, Long commentId) {
        Optional<CommentReaction> reaction = commentReactionPersistencePort
                .findByCommentIdAndUserEmail(commentId, userEmail);
        return reaction.isPresent() && reaction.get().isDislike();
    }

    @Override
    public PaginationResponse<Map<String, Object>> getCommentsByUserEmail(String userEmail, int page, int size) {
        log.debug("사용자 댓글 조회: userEmail={}, page={}, size={}", userEmail, page, size);

        try {
            Comments comments = commentPersistencePort.findByUserEmail(userEmail, page, size);
            List<Map<String, Object>> content = comments.getComments().stream()
                    .map(this::toMyCommentRow)
                    .collect(Collectors.toList());
            return PaginationResponse.<Map<String, Object>>builder()
                    .content(content)
                    .page(page)
                    .size(size)
                    .totalElements(comments.getTotalElements())
                    .build();
        } catch (Exception e) {
            log.error("사용자 댓글 조회 실패: userEmail={}", userEmail, e);
            throw new SystemException("사용자 댓글 조회 중 오류가 발생했습니다", "USER_COMMENTS_ERROR", e);
        }
    }

    @Override
    public PaginationResponse<Map<String, Object>> getMyReactions(String userEmail, int page, int size) {
        long total = commentReactionPersistencePort.countByUserEmail(userEmail);
        List<Map<String, Object>> content = commentReactionPersistencePort.findByUserEmail(userEmail, page, size)
                .stream()
                .map(reaction -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("commentId", reaction.getCommentId());
                    row.put("reactionType", reaction.getReactionType() != null
                            ? reaction.getReactionType().name()
                            : null);
                    row.put("createdAt", reaction.getCreatedAt());
                    commentPersistencePort.findById(reaction.getCommentId()).ifPresentOrElse(comment -> {
                        String contentText = comment.getContent() != null ? comment.getContent().trim() : "";
                        if (contentText.length() > 80) {
                            contentText = contentText.substring(0, 80) + "…";
                        }
                        row.put("content", contentText.isEmpty() ? "(내용 없음)" : contentText);
                        row.put("targetId", comment.getTargetId());
                        row.put("commentType", comment.getCommentType() != null
                                ? comment.getCommentType().name()
                                : null);
                        putBoardType(row, comment.getCommentType(), comment.getTargetId());
                    }, () -> {
                        row.put("content", "(삭제된 댓글)");
                        row.put("targetId", null);
                        row.put("commentType", null);
                        row.put("boardType", null);
                    });
                    return row;
                })
                .collect(Collectors.toList());
        return PaginationResponse.<Map<String, Object>>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(total)
                .build();
    }

    private Map<String, Object> toMyCommentRow(Comment comment) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", comment.getId());
        row.put("content", comment.getContent());
        row.put("targetId", comment.getTargetId());
        row.put("commentType", comment.getCommentType() != null ? comment.getCommentType().name() : null);
        row.put("createdAt", comment.getCreatedAt());
        putBoardType(row, comment.getCommentType(), comment.getTargetId());
        return row;
    }

    private void putBoardType(Map<String, Object> row, CommentType commentType, Long targetId) {
        if (commentType != CommentType.BOARD || targetId == null) {
            row.put("boardType", null);
            return;
        }
        boardPersistencePort.findById(targetId).ifPresentOrElse(
                board -> row.put("boardType", board.getBoardType() != null ? board.getBoardType().name() : null),
                () -> row.put("boardType", null));
    }

}
