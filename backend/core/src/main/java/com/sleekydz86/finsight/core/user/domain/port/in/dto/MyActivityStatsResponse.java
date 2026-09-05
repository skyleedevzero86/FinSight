package com.sleekydz86.finsight.core.user.domain.port.in.dto;

public class MyActivityStatsResponse {
    private long boardCount;
    private long commentCount;
    private long boardReactionCount;
    private long commentReactionCount;
    private String rangeFrom;
    private String rangeTo;
    private String periodType;

    public MyActivityStatsResponse() {
    }

    public MyActivityStatsResponse(
            long boardCount,
            long commentCount,
            long boardReactionCount,
            long commentReactionCount,
            String rangeFrom,
            String rangeTo,
            String periodType) {
        this.boardCount = boardCount;
        this.commentCount = commentCount;
        this.boardReactionCount = boardReactionCount;
        this.commentReactionCount = commentReactionCount;
        this.rangeFrom = rangeFrom;
        this.rangeTo = rangeTo;
        this.periodType = periodType;
    }

    public long getBoardCount() {
        return boardCount;
    }

    public void setBoardCount(long boardCount) {
        this.boardCount = boardCount;
    }

    public long getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(long commentCount) {
        this.commentCount = commentCount;
    }

    public long getBoardReactionCount() {
        return boardReactionCount;
    }

    public void setBoardReactionCount(long boardReactionCount) {
        this.boardReactionCount = boardReactionCount;
    }

    public long getCommentReactionCount() {
        return commentReactionCount;
    }

    public void setCommentReactionCount(long commentReactionCount) {
        this.commentReactionCount = commentReactionCount;
    }

    public String getRangeFrom() {
        return rangeFrom;
    }

    public void setRangeFrom(String rangeFrom) {
        this.rangeFrom = rangeFrom;
    }

    public String getRangeTo() {
        return rangeTo;
    }

    public void setRangeTo(String rangeTo) {
        this.rangeTo = rangeTo;
    }

    public String getPeriodType() {
        return periodType;
    }

    public void setPeriodType(String periodType) {
        this.periodType = periodType;
    }
}
