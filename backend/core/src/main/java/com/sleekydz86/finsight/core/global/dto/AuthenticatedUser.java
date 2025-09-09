package com.sleekydz86.finsight.core.global.dto;

import com.sleekydz86.finsight.core.user.domain.UserRole;

public class AuthenticatedUser {
    private final Long id;
    private final String email;
    private final String username;
    private final UserRole role;

    public AuthenticatedUser(Long id, String email, String username, UserRole role) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.role = role;
    }

    public static AuthenticatedUser of(Long id, String email, String username, UserRole role) {
        return new AuthenticatedUser(id, email, username, role);
    }

    public static AuthenticatedUser system() {
        return new AuthenticatedUser(0L, "system@finsight.com", "system", UserRole.ADMIN);
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isSystem() {
        return id == 0L;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isUser() {
        return role == UserRole.USER;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthenticatedUser that = (AuthenticatedUser) o;
        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "AuthenticatedUser{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", username='" + username + '\'' +
                ", role=" + role +
                '}';
    }
}