package com.sleekydz86.finsight.core.user.domain.port.out.dto;

import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.user.domain.NotificationType;
import com.sleekydz86.finsight.core.user.domain.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class UserDashboardResponse {

    private UserResponse profile;
    private List<TargetCategory> watchlist;
    private List<NotificationType> notificationPreferences;
    private LocalDateTime lastLoginAt;
    private LocalDateTime accountCreatedAt;

    public static UserDashboardResponse of(User user, List<TargetCategory> watchlist,
            List<NotificationType> notificationPreferences) {
        return UserDashboardResponse.builder()
                .profile(UserResponse.from(user))
                .watchlist(watchlist)
                .notificationPreferences(notificationPreferences)
                .lastLoginAt(user.getLastLoginAt())
                .accountCreatedAt(user.getCreatedAt())
                .build();
    }
}
