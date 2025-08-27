package com.sleekydz86.finsight.core.auth.dto;

import com.sleekydz86.finsight.core.user.domain.NotificationType;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class UserRegistrationRequest {
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 12, message = "비밀번호는 최소 12자 이상이어야 합니다")
    private String password;

    @NotBlank(message = "사용자명은 필수입니다")
    @Size(min = 2, max = 50, message = "사용자명은 2자 이상 50자 이하여야 합니다")
    private String username;

    private List<TargetCategory> watchlist;
    private List<NotificationType> notificationPreferences;

    public UserRegistrationRequest() {}

    public UserRegistrationRequest(String email, String password, String username) {
        this.email = email;
        this.password = password;
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<TargetCategory> getWatchlist() {
        return watchlist;
    }

    public void setWatchlist(List<TargetCategory> watchlist) {
        this.watchlist = watchlist;
    }

    public List<NotificationType> getNotificationPreferences() {
        return notificationPreferences;
    }

    public void setNotificationPreferences(List<NotificationType> notificationPreferences) {
        this.notificationPreferences = notificationPreferences;
    }
}