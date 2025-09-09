package com.sleekydz86.finsight.core.global.dto;

import com.sleekydz86.finsight.core.user.domain.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticatedUser {
    private Long id;
    private String email;
    private String nickname;
    private String role;

    public static AuthenticatedUserBuilder builder() {
        return new AuthenticatedUserBuilder();
    }

    public static class AuthenticatedUserBuilder {
        private Long id;
        private String email;
        private String nickname;
        private String role;

        public AuthenticatedUserBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public AuthenticatedUserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public AuthenticatedUserBuilder nickname(String nickname) {
            this.nickname = nickname;
            return this;
        }

        public AuthenticatedUserBuilder role(String role) {
            this.role = role;
            return this;
        }

        public AuthenticatedUser build() {
            return new AuthenticatedUser(id, email, nickname, role);
        }
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getNickname() {
        return nickname;
    }

    public String getRole() {
        return role;
    }

    @Override
    public String toString() {
        return "AuthenticatedUser{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", nickname='" + nickname + '\'' +
                ", role='" + role + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthenticatedUser that = (AuthenticatedUser) o;
        return java.util.Objects.equals(id, that.id) &&
                java.util.Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, email);
    }
}