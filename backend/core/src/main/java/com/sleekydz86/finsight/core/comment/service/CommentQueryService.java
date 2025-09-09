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
import com.sleekydz86.finsight.core.global.exception.NewsNotFoundException;
import com.sleekydz86.finsight.core.global.exception.SystemException;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsQueryRequest;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CommentQueryService implements CommentQueryUseCase {

    private static final Logger log = LoggerFactory.getLogger(CommentQueryService.class);

    private final CommentPersistencePort commentPersistencePort;
    private final CommentReactionPersistencePort commentReactionPersistencePort;
    private final CommentReportPersistencePort commentReportPersistencePort;

    public CommentQueryService(CommentPersistencePort commentPersistencePort,
                               CommentReactionPersistencePort commentReactionPersistencePort,
                               CommentReportPersistencePort commentReportPersistencePort) {
        this.commentPersistencePort = commentPersistencePort;
        this.commentReactionPersistencePort = commentReactionPersistencePort;
        this.commentReportPersistencePort = commentReportPersistencePort;
    }

    @Override
    public Comments getCommentsByTargetId(Long targetId, CommentType commentType) {
        log.info("Getting comments for targetId: {}, type: {}", targetId, commentType);

        Comments comments = commentPersistencePort.findByTargetIdAndType(targetId, commentType);

        Comments commentsWithReplies = new Comments();
        for (Comment comment : comments.getComments()) {
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
        log.info("Getting comments with pagination for targetId: {}, type: {}, page: {}, size: {}",
                targetId, commentType, page, size);

        Comments comments = commentPersistencePort.findByTargetIdAndTypeWithPagination(targetId, commentType, page, size);

        Comments commentsWithReplies = new Comments();
        for (Comment comment : comments.getComments()) {
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
        log.info("Getting comment by ID: {}", commentId);

        return commentPersistencePort.findById(commentId)
                .orElseThrow(() -> new NewsNotFoundException(commentId));
    }

    @Override
    public List<CommentResponse> getCommentReplies(Long parentId) {
        log.info("Getting replies for parent comment: {}", parentId);

        List<Comment> replies = commentPersistencePort.findRepliesByParentId(parentId);
        return replies.stream()
                .map(CommentResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public Comments getCommentsByUser(String userEmail) {
        log.info("Getting comments by user: {}", userEmail);

        return commentPersistencePort.findByUserEmail(userEmail);
    }

    @Override
    public Comments getReportedComments() {
        log.info("Getting reported comments");

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
    public List<CommentResponse> getCommentsByUserEmail(String userEmail, int page, int size) {
        log.debug("사용자 댓글 조회: userEmail={}, page={}, size={}", userEmail, page, size);

        try {
            Comments comments = commentPersistencePort.findByUserEmail(userEmail);

            return comments.getComments().stream()
                    .map(CommentResponse::from)
                    .skip((long) page * size)
                    .limit(size)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("사용자 댓글 조회 실패: userEmail={}", userEmail, e);
            throw new SystemException("사용자 댓글 조회 중 오류가 발생했습니다", "USER_COMMENTS_ERROR", e);
        }
    }

}
