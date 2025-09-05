package com.sleekydz86.finsight.core.board.domain.port.in.dto;

public class BoardAuthorStatisticsResponse {
    private final String authorEmail;
    private final long boardCount;
    private final long totalViews;
    private final long totalLikes;
    private final long totalComments;
    private final double averageViewsPerBoard;
    private final double averageLikesPerBoard;

    public BoardAuthorStatisticsResponse() {
        this.authorEmail = "";
        this.boardCount = 0;
        this.totalViews = 0;
        this.totalLikes = 0;
        this.totalComments = 0;
        this.averageViewsPerBoard = 0.0;
        this.averageLikesPerBoard = 0.0;
    }

    public BoardAuthorStatisticsResponse(String authorEmail, long boardCount, long totalViews,
                                         long totalLikes, long totalComments,
                                         double averageViewsPerBoard, double averageLikesPerBoard) {
        this.authorEmail = authorEmail;
        this.boardCount = boardCount;
        this.totalViews = totalViews;
        this.totalLikes = totalLikes;
        this.totalComments = totalComments;
        this.averageViewsPerBoard = averageViewsPerBoard;
        this.averageLikesPerBoard = averageLikesPerBoard;
    }

    public String getAuthorEmail() { return authorEmail; }
    public long getBoardCount() { return boardCount; }
    public long getTotalViews() { return totalViews; }
    public long getTotalLikes() { return totalLikes; }
    public long getTotalComments() { return totalComments; }
    public double getAverageViewsPerBoard() { return averageViewsPerBoard; }
    public double getAverageLikesPerBoard() { return averageLikesPerBoard; }
}