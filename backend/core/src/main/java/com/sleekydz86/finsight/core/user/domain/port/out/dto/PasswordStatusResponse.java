package com.sleekydz86.finsight.core.user.domain.port.out.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordStatusResponse {

    private boolean isChangeRequired;

    private boolean isChangeRecommended;

    private long todayChangeCount;

    private Long daysSinceLastChange;

    private Long daysUntilExpiry;

    private int maxDailyChanges;

    private Integer strengthScore;

    private String statusMessage;

    public static PasswordStatusResponse of(boolean isChangeRequired,
                                            boolean isChangeRecommended,
                                            long todayChangeCount) {
        return PasswordStatusResponse.builder()
                .isChangeRequired(isChangeRequired)
                .isChangeRecommended(isChangeRecommended)
                .todayChangeCount(todayChangeCount)
                .maxDailyChanges(3)
                .statusMessage(isChangeRequired ? "비밀번호 변경이 필요합니다" : "비밀번호 변경이 권장됩니다")
                .build();
    }

    public static PasswordStatusResponse of(boolean isChangeRequired,
                                            boolean isChangeRecommended,
                                            long todayChangeCount,
                                            Long daysSinceLastChange,
                                            Long daysUntilExpiry,
                                            int maxDailyChanges,
                                            Integer strengthScore,
                                            String statusMessage) {
        return PasswordStatusResponse.builder()
                .isChangeRequired(isChangeRequired)
                .isChangeRecommended(isChangeRecommended)
                .todayChangeCount(todayChangeCount)
                .daysSinceLastChange(daysSinceLastChange)
                .daysUntilExpiry(daysUntilExpiry)
                .maxDailyChanges(maxDailyChanges)
                .strengthScore(strengthScore)
                .statusMessage(statusMessage)
                .build();
    }

    public boolean canChangeToday() {
        return todayChangeCount < maxDailyChanges;
    }

    public int getRemainingChangesToday() {
        return Math.max(0, maxDailyChanges - (int) todayChangeCount);
    }

    public boolean isPasswordHealthy() {
        return !isChangeRequired && !isChangeRecommended && todayChangeCount == 0;
    }
}