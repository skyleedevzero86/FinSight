package com.sleekydz86.finsight.core.user.domain;

import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "username", nullable = false)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ElementCollection(targetClass = TargetCategory.class)
    @CollectionTable(
            name = "user_watchlist",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private List<TargetCategory> watchlist = new ArrayList<>();

    @ElementCollection(targetClass = NotificationType.class)
    @CollectionTable(
            name = "user_notification_preferences",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private List<NotificationType> notificationPreferences = new ArrayList<>();

    public User() {}

    public User(String email, String password, String username) {
        this.email = email;
        this.password = password;
        this.username = username;
        this.role = UserRole.USER;
    }

    // JPA Entity에서 Domain으로 변환할 때 사용하는 생성자
    public User(Long id, String email, String password, String username, UserRole role,
                boolean isActive, LocalDateTime lastLoginAt, List<TargetCategory> watchlist,
                List<NotificationType> notificationPreferences) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.username = username;
        this.role = role;
        this.isActive = isActive;
        this.lastLoginAt = lastLoginAt;
        this.watchlist = watchlist != null ? new ArrayList<>(watchlist) : new ArrayList<>();
        this.notificationPreferences = notificationPreferences != null ? new ArrayList<>(notificationPreferences) : new ArrayList<>();
    }

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void addToWatchlist(TargetCategory category) {
        if (!this.watchlist.contains(category)) {
            this.watchlist.add(category);
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void removeFromWatchlist(TargetCategory category) {
        this.watchlist.remove(category);
        this.updatedAt = LocalDateTime.now();
    }

    public void updateNotificationPreferences(List<NotificationType> preferences) {
        this.notificationPreferences = new ArrayList<>(preferences);
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<TargetCategory> getWatchlist() { return new ArrayList<>(watchlist); }
    public void setWatchlist(List<TargetCategory> watchlist) { this.watchlist = new ArrayList<>(watchlist); }
    public List<NotificationType> getNotificationPreferences() { return new ArrayList<>(notificationPreferences); }
    public void setNotificationPreferences(List<NotificationType> notificationPreferences) { this.notificationPreferences = new ArrayList<>(notificationPreferences); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) && Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }
}