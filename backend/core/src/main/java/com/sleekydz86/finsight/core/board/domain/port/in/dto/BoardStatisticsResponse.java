package com.sleekydz86.finsight.core.board.domain.port.in.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class BoardStatisticsResponse {
    private final long totalBoards;
    private final long totalViews;
    private final long totalLikes;
    private final long totalComments;
    private final long totalReports;
    private final Map<String, Long> boardsByType;
    private final Map<String, Long> boardsByStatus;
    private final Map<String, Long> dailyBoardCount;
    private final Map<String, Long> weeklyBoardCount;
    private final Map<String, Long> monthlyBoardCount;
    private final LocalDateTime lastUpdated;

    public BoardStatisticsResponse() {
        this.totalBoards = 0;
        this.totalViews = 0;
        this.totalLikes = 0;
        this.totalComments = 0;
        this.totalReports = 0;
        this.boardsByType = Map.of();
        this.boardsByStatus = Map.of();
        this.dailyBoardCount = Map.of();
        this.weeklyBoardCount = Map.of();
        this.monthlyBoardCount = Map.of();
        this.lastUpdated = LocalDateTime.now();
    }

    public BoardStatisticsResponse(long totalBoards, long totalViews, long totalLikes,
                                   long totalComments, long totalReports,
                                   Map<String, Long> boardsByType, Map<String, Long> boardsByStatus,
                                   Map<String, Long> dailyBoardCount, Map<String, Long> weeklyBoardCount,
                                   Map<String, Long> monthlyBoardCount, LocalDateTime lastUpdated) {
        this.totalBoards = totalBoards;
        this.totalViews = totalViews;
        this.totalLikes = totalLikes;
        this.totalComments = totalComments;
        this.totalReports = totalReports;
        this.boardsByType = boardsByType != null ? boardsByType : Map.of();
        this.boardsByStatus = boardsByStatus != null ? boardsByStatus : Map.of();
        this.dailyBoardCount = dailyBoardCount != null ? dailyBoardCount : Map.of();
        this.weeklyBoardCount = weeklyBoardCount != null ? weeklyBoardCount : Map.of();
        this.monthlyBoardCount = monthlyBoardCount != null ? monthlyBoardCount : Map.of();
        this.lastUpdated = lastUpdated;
    }

    public long getTotalBoards() { return totalBoards; }
    public long getTotalViews() { return totalViews; }
    public long getTotalLikes() { return totalLikes; }
    public long getTotalComments() { return totalComments; }
    public long getTotalReports() { return totalReports; }
    public Map<String, Long> getBoardsByType() { return boardsByType; }
    public Map<String, Long> getBoardsByStatus() { return boardsByStatus; }
    public Map<String, Long> getDailyBoardCount() { return dailyBoardCount; }
    public Map<String, Long> getWeeklyBoardCount() { return weeklyBoardCount; }
    public Map<String, Long> getMonthlyBoardCount() { return monthlyBoardCount; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
}