package com.sleekydz86.finsight.core.comment.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommentHideAuthorTest {

    @Test
    void hidesAuthorOnCommentAndReply() {
        Comment reply = Comment.builder()
                .id(2L)
                .content("답글")
                .authorEmail("reply@example.com")
                .commentType(CommentType.NEWS)
                .targetId(10L)
                .build();
        Comment comment = Comment.builder()
                .id(1L)
                .content("본문")
                .authorEmail("writer@example.com")
                .commentType(CommentType.NEWS)
                .targetId(10L)
                .build()
                .addReply(reply);

        Comment hidden = comment.hideAuthor();

        assertThat(hidden.getAuthorEmail()).isEqualTo("작성자");
        assertThat(hidden.getContent()).isEqualTo("본문");
        assertThat(hidden.getReplies()).hasSize(1);
        assertThat(hidden.getReplies().get(0).getAuthorEmail()).isEqualTo("작성자");
    }
}
