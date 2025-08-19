package com.sleekydz86.finsight.core.user.domain.port.in.dto;

import java.util.Objects;

public class UserRegistrationRequest {
    private final String email;
    private final String password;
    private final String username;

    public UserRegistrationRequest(String email, String password, String username) {
        this.email = email;
        this.password = password;
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserRegistrationRequest that = (UserRegistrationRequest) o;
        return Objects.equals(email, that.email) &&
                Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, username);
    }
}