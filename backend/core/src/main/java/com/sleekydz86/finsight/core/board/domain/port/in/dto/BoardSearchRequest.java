package com.sleekydz86.finsight.core.board.domain.port.in.dto;

import com.sleekydz86.finsight.core.board.domain.BoardType;

import java.time.LocalDateTime;
import java.util.List;

public class BoardSearchRequest {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BoardType boardType;
    private String keyword;
    private String hashtag;
    private SearchType searchType;
    private List<String> hashtags;
    private int page = 0;
    private int size = 20;
    private String sortBy = "createdAt";
    private String sortDirection = "desc";

    public BoardSearchRequest() {
    }

    public BoardSearchRequest(LocalDateTime startDate, LocalDateTime endDate, BoardType boardType,
            String keyword, SearchType searchType, List<String> hashtags, int page, int size) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.boardType = boardType;
        this.keyword = keyword;
        this.searchType = searchType;
        this.hashtags = hashtags;
        this.page = page;
        this.size = size;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private BoardSearchRequest request = new BoardSearchRequest();

        public Builder startDate(LocalDateTime startDate) {
            request.startDate = startDate;
            return this;
        }

        public Builder endDate(LocalDateTime endDate) {
            request.endDate = endDate;
            return this;
        }

        public Builder boardType(BoardType boardType) {
            request.boardType = boardType;
            return this;
        }

        public Builder keyword(String keyword) {
            request.keyword = keyword;
            return this;
        }

        public Builder hashtag(String hashtag) {
            request.hashtag = hashtag;
            return this;
        }

        public Builder searchType(SearchType searchType) {
            request.searchType = searchType;
            return this;
        }

        public Builder hashtags(List<String> hashtags) {
            request.hashtags = hashtags;
            return this;
        }

        public Builder page(int page) {
            request.page = page;
            return this;
        }

        public Builder size(int size) {
            request.size = size;
            return this;
        }

        public Builder sortBy(String sortBy) {
            request.sortBy = sortBy;
            return this;
        }

        public Builder sortDirection(String sortDirection) {
            request.sortDirection = sortDirection;
            return this;
        }

        public BoardSearchRequest build() {
            return request;
        }
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public BoardType getBoardType() {
        return boardType;
    }

    public void setBoardType(BoardType boardType) {
        this.boardType = boardType;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getHashtag() {
        return hashtag;
    }

    public void setHashtag(String hashtag) {
        this.hashtag = hashtag;
    }

    public SearchType getSearchType() {
        return searchType;
    }

    public void setSearchType(SearchType searchType) {
        this.searchType = searchType;
    }

    public List<String> getHashtags() {
        return hashtags;
    }

    public void setHashtags(List<String> hashtags) {
        this.hashtags = hashtags;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }

    public enum SearchType {
        TITLE("제목"),
        CONTENT("내용"),
        HASHTAG("해시태그"),
        ALL("전체");

        private final String description;

        SearchType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}