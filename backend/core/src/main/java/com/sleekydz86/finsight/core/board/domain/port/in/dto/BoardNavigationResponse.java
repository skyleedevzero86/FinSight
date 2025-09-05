package com.sleekydz86.finsight.core.board.domain.port.in.dto;

public class BoardNavigationResponse {
    private final BoardNavigationItem previous;
    private final BoardNavigationItem next;

    public BoardNavigationResponse() {
        this.previous = null;
        this.next = null;
    }

    public BoardNavigationResponse(BoardNavigationItem previous, BoardNavigationItem next) {
        this.previous = previous;
        this.next = next;
    }

    public BoardNavigationItem getPrevious() { return previous; }
    public BoardNavigationItem getNext() { return next; }

    public static class BoardNavigationItem {
        private final Long id;
        private final String title;
        private final String authorEmail;
        private final String createdAt;

        public BoardNavigationItem() {
            this.id = null;
            this.title = "";
            this.authorEmail = "";
            this.createdAt = "";
        }

        public BoardNavigationItem(Long id, String title, String authorEmail, String createdAt) {
            this.id = id;
            this.title = title;
            this.authorEmail = authorEmail;
            this.createdAt = createdAt;
        }

        public Long getId() { return id; }
        public String getTitle() { return title; }
        public String getAuthorEmail() { return authorEmail; }
        public String getCreatedAt() { return createdAt; }
    }
}