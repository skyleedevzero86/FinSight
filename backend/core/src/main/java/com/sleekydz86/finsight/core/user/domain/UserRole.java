package com.sleekydz86.finsight.core.user.domain;

public enum UserRole {
    USER("일반 사용자"),
    MANAGER("관리자"),
    ADMIN("시스템 관리자");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean hasAdminPrivileges() {
        return this == ADMIN;
    }

    public boolean hasManagerPrivileges() {
        return this == ADMIN || this == MANAGER;
    }

    public boolean canManageUsers() {
        return this == ADMIN || this == MANAGER;
    }
}