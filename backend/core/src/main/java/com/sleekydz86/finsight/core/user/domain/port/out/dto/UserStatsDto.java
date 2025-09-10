package com.sleekydz86.finsight.core.user.domain.port.out.dto;

import com.sleekydz86.finsight.core.user.domain.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsDto {
    private UserStatus status;
    private Long count;
    private Long totalUsers;
    private Long activeUsers;
    private Long pendingUsers;
    private Long suspendedUsers;
    private Long withdrawnUsers;
    private Long rejectedUsers;
    private Long adminUsers;
    private Long managerUsers;
    private Long regularUsers;
    private Long premiumUsers;
    private Long usersWithPasswordChangeRequired;
    private Long usersWithPasswordChangeRecommended;
    private Long usersCreatedToday;
    private Long usersCreatedThisWeek;
    private Long usersCreatedThisMonth;
    private Long usersLoggedInToday;
    private Long usersLoggedInThisWeek;
    private Long usersLoggedInThisMonth;

    public UserStatsDto(UserStatus status, Long count) {
        this.status = status;
        this.count = count;
    }

}