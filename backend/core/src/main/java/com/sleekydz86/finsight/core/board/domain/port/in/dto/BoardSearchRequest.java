package com.sleekydz86.finsight.core.board.domain.port.in.dto;

import com.sleekydz86.finsight.core.board.domain.BoardType;

import java.time.LocalDateTime;
import java.util.List;

public class BoardSearchRequest {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BoardType boardType;
    private String keyword;
    private SearchType searchType;
    private List<String> hashtags;
    private int page = 0;
    private int size = 20;

    public BoardSearchRequest() {}

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

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public BoardType getBoardType() { return boardType; }
    public void setBoardType(BoardType boardType) { this.boardType = boardType; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public SearchType getSearchType() { return searchType; }
    public void setSearchType(SearchType searchType) { this.searchType = searchType; }

    public List<String> getHashtags() { return hashtags; }
    public void setHashtags(List<String> hashtags) { this.hashtags = hashtags; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

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