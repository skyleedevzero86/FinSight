package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.comment.domain.Comment;
import com.sleekydz86.finsight.core.comment.domain.Comments;
import com.sleekydz86.finsight.core.comment.domain.CommentType;
import com.sleekydz86.finsight.core.comment.domain.port.in.CommentCommandUseCase;
import com.sleekydz86.finsight.core.comment.domain.port.in.CommentQueryUseCase;
import com.sleekydz86.finsight.core.comment.domain.port.in.dto.CommentCreateRequest;
import com.sleekydz86.finsight.core.comment.domain.port.in.dto.CommentUpdateRequest;
import com.sleekydz86.finsight.core.comment.domain.port.in.dto.CommentReportRequest;
import com.sleekydz86.finsight.core.comment.domain.port.in.dto.CommentResponse;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@CrossOrigin(origins = "*")
public class CommentController {

    private static final Logger log = LoggerFactory.getLogger(CommentController.class);

    private final CommentCommandUseCase commentCommandUseCase;
    private final CommentQueryUseCase commentQueryUseCase;

    public CommentController(CommentCommandUseCase commentCommandUseCase,
            CommentQueryUseCase commentQueryUseCase) {
        this.commentCommandUseCase = commentCommandUseCase;
        this.commentQueryUseCase = commentQueryUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Comment>> createComment(
            @Valid @RequestBody CommentCreateRequest request,
            Authentication authentication) {

        String userEmail = authentication.getName();
        log.info("Creating comment by user: {}", userEmail);

        Comment comment = commentCommandUseCase.createComment(userEmail, request);

        return ResponseEntity.ok(ApiResponse.success(comment, "댓글이 작성되었습니다"));
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Comment>> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request,
            Authentication authentication) {

        String userEmail = authentication.getName();
        log.info("Updating comment: {} by user: {}", commentId, userEmail);

        Comment comment = commentCommandUseCase.updateComment(userEmail, commentId, request);

        return ResponseEntity.ok(ApiResponse.success(comment, "댓글이 수정되었습니다"));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            Authentication authentication) {

        String userEmail = authentication.getName();
        log.info("Deleting comment: {} by user: {}", commentId, userEmail);

        commentCommandUseCase.deleteComment(userEmail, commentId);

        return ResponseEntity.ok(ApiResponse.success(null, "댓글이 삭제되었습니다"));
    }

    @PostMapping("/{commentId}/like")
    public ResponseEntity<ApiResponse<Comment>> likeComment(
            @PathVariable Long commentId,
            Authentication authentication) {

        String userEmail = authentication.getName();
        log.info("Liking comment: {} by user: {}", commentId, userEmail);

        Comment comment = commentCommandUseCase.likeComment(userEmail, commentId);

        return ResponseEntity.ok(ApiResponse.success(comment, "댓글에 좋아요를 눌렀습니다"));
    }

    @PostMapping("/{commentId}/dislike")
    public ResponseEntity<ApiResponse<Comment>> dislikeComment(
            @PathVariable Long commentId,
            Authentication authentication) {

        String userEmail = authentication.getName();
        log.info("Disliking comment: {} by user: {}", commentId, userEmail);

        Comment comment = commentCommandUseCase.dislikeComment(userEmail, commentId);

        return ResponseEntity.ok(ApiResponse.success(comment, "댓글에 싫어요를 눌렀습니다"));
    }

    @PostMapping("/{commentId}/report")
    public ResponseEntity<ApiResponse<Void>> reportComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentReportRequest request,
            Authentication authentication) {

        String userEmail = authentication.getName();
        log.info("Reporting comment: {} by user: {}", commentId, userEmail);

        commentCommandUseCase.reportComment(userEmail, commentId, request);

        return ResponseEntity.ok(ApiResponse.success(null, "댓글이 신고되었습니다"));
    }

    // 타겟별 댓글 조회 (뉴스, 게시판 등)
    @GetMapping("/target/{targetId}")
    public ResponseEntity<ApiResponse<Comments>> getCommentsByTarget(
            @PathVariable Long targetId,
            @RequestParam(defaultValue = "NEWS") String commentType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Getting comments for target: {}, type: {}", targetId, commentType);

        CommentType type = CommentType.valueOf(commentType.toUpperCase());
        Comments comments = commentQueryUseCase.getCommentsByTargetIdWithPagination(targetId, type, page, size);

        return ResponseEntity.ok(ApiResponse.success(comments, "댓글 목록을 조회했습니다"));
    }

    // 뉴스 댓글 조회
    @GetMapping("/news/{newsId}")
    public ResponseEntity<ApiResponse<Comments>> getNewsComments(
            @PathVariable Long newsId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Getting comments for news: {}, page: {}, size: {}", newsId, page, size);

        Comments comments = commentQueryUseCase.getCommentsByTargetIdWithPagination(
                newsId, CommentType.NEWS, page, size);

        return ResponseEntity.ok(ApiResponse.success(comments, "뉴스 댓글을 조회했습니다"));
    }

    // 게시판 댓글 조회
    @GetMapping("/board/{boardId}")
    public ResponseEntity<ApiResponse<Comments>> getBoardComments(
            @PathVariable Long boardId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Getting comments for board: {}, page: {}, size: {}", boardId, page, size);

        Comments comments = commentQueryUseCase.getCommentsByTargetIdWithPagination(
                boardId, CommentType.BOARD, page, size);

        return ResponseEntity.ok(ApiResponse.success(comments, "게시판 댓글을 조회했습니다"));
    }

    @GetMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Comment>> getCommentById(@PathVariable Long commentId) {
        log.info("Getting comment by ID: {}", commentId);

        Comment comment = commentQueryUseCase.getCommentById(commentId);

        return ResponseEntity.ok(ApiResponse.success(comment, "댓글을 조회했습니다"));
    }

    @GetMapping("/{commentId}/replies")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getCommentReplies(@PathVariable Long commentId) {
        log.info("Getting replies for comment: {}", commentId);

        List<CommentResponse> replies = commentQueryUseCase.getCommentReplies(commentId);

        return ResponseEntity.ok(ApiResponse.success(replies, "대댓글 목록을 조회했습니다"));
    }

    @GetMapping("/user/{userEmail}")
    public ResponseEntity<ApiResponse<Comments>> getCommentsByUser(@PathVariable String userEmail) {
        log.info("Getting comments by user: {}", userEmail);

        Comments comments = commentQueryUseCase.getCommentsByUser(userEmail);

        return ResponseEntity.ok(ApiResponse.success(comments, "사용자 댓글 목록을 조회했습니다"));
    }

    @GetMapping("/reported")
    public ResponseEntity<ApiResponse<Comments>> getReportedComments() {
        log.info("Getting reported comments");

        Comments comments = commentQueryUseCase.getReportedComments();

        return ResponseEntity.ok(ApiResponse.success(comments, "신고된 댓글 목록을 조회했습니다"));
    }

    @GetMapping("/{commentId}/reaction-status")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getReactionStatus(
            @PathVariable Long commentId,
            Authentication authentication) {

        String userEmail = authentication.getName();
        log.info("Getting reaction status for comment: {} by user: {}", commentId, userEmail);

        boolean hasLiked = commentQueryUseCase.hasUserLikedComment(userEmail, commentId);
        boolean hasDisliked = commentQueryUseCase.hasUserDislikedComment(userEmail, commentId);

        Map<String, Boolean> reactionStatus = Map.of(
                "hasLiked", hasLiked,
                "hasDisliked", hasDisliked);

        return ResponseEntity.ok(ApiResponse.success(reactionStatus, "반응 상태를 조회했습니다"));
    }
}