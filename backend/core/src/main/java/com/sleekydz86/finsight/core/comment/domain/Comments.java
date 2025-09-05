package com.sleekydz86.finsight.core.comment.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Comments {
    private final List<Comment> comments;

    public Comments() {
        this.comments = new ArrayList<>();
    }

    public Comments(List<Comment> comments) {
        this.comments = comments != null ? new ArrayList<>(comments) : new ArrayList<>();
    }

    public Comments addComment(Comment comment) {
        List<Comment> newComments = new ArrayList<>(this.comments);
        newComments.add(comment);
        return new Comments(newComments);
    }

    public Comments removeComment(Long commentId) {
        List<Comment> newComments = this.comments.stream()
                .filter(comment -> !Objects.equals(comment.getId(), commentId))
                .collect(Collectors.toList());
        return new Comments(newComments);
    }

    public Comments updateComment(Long commentId, Comment updatedComment) {
        List<Comment> newComments = this.comments.stream()
                .map(comment -> Objects.equals(comment.getId(), commentId) ? updatedComment : comment)
                .collect(Collectors.toList());
        return new Comments(newComments);
    }

    public List<Comment> getActiveComments() {
        return comments.stream()
                .filter(Comment::isActive)
                .collect(Collectors.toList());
    }

    public List<Comment> getCommentsByType(CommentType commentType) {
        return comments.stream()
                .filter(comment -> comment.getCommentType() == commentType)
                .collect(Collectors.toList());
    }

    public List<Comment> getCommentsByTargetId(Long targetId) {
        return comments.stream()
                .filter(comment -> Objects.equals(comment.getTargetId(), targetId))
                .collect(Collectors.toList());
    }

    public List<Comment> getParentComments() {
        return comments.stream()
                .filter(comment -> !comment.isReply())
                .collect(Collectors.toList());
    }

    public List<Comment> getRepliesByParentId(Long parentId) {
        return comments.stream()
                .filter(comment -> Objects.equals(comment.getParentId(), parentId))
                .collect(Collectors.toList());
    }

    public int getTotalCount() {
        return comments.size();
    }

    public int getActiveCount() {
        return (int) comments.stream().filter(Comment::isActive).count();
    }

    public boolean isEmpty() {
        return comments.isEmpty();
    }

    public List<Comment> getComments() {
        return new ArrayList<>(comments);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Comments comments1 = (Comments) o;
        return Objects.equals(comments, comments1.comments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(comments);
    }

    @Override
    public String toString() {
        return "Comments{" +
                "comments=" + comments +
                '}';
    }
}