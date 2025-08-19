package com.sleekydz86.finsight.core.user.domain.port.in.dto;

import java.util.Objects;

public class UserUpdateRequest {
    private final String username;
    private final String password;

    public UserUpdateRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserUpdateRequest that = (UserUpdateRequest) o;
        return Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}