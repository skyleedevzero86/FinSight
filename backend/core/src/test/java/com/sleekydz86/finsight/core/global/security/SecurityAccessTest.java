package com.sleekydz86.finsight.core.global.security;

import com.sleekydz86.finsight.core.global.exception.InsufficientPermissionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityAccessTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminAndManagerAuthoritiesAreAccepted() {
        authenticate("ROLE_ADMIN");
        assertThat(SecurityAccess.isAdminOrManager()).isTrue();
        assertThatCode(SecurityAccess::requireAdminOrManager).doesNotThrowAnyException();

        authenticate("ROLE_MANAGER");
        assertThat(SecurityAccess.isAdminOrManager()).isTrue();
        assertThatCode(SecurityAccess::requireAdminOrManager).doesNotThrowAnyException();
    }

    @Test
    void roleMatchIsExactAndDoesNotAcceptPlainOrLookalikeAuthorities() {
        authenticate("ADMIN", "ROLE_ADMINISTRATOR", "SCOPE_ROLE_MANAGER");

        assertThat(SecurityAccess.isAdminOrManager()).isFalse();
    }

    @Test
    void unauthenticatedTokenIsRejectedEvenWithAdminAuthority() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                "user", "password", "ROLE_ADMIN");
        authentication.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(SecurityAccess.isAdminOrManager()).isFalse();
    }

    @Test
    void missingAuthenticationIsRejected() {
        assertThat(SecurityAccess.isAdminOrManager()).isFalse();
    }

    @Test
    void requireAdminOrManagerThrowsDomainExceptionForRegularUser() {
        authenticate("ROLE_USER");

        assertThatThrownBy(SecurityAccess::requireAdminOrManager)
                .isInstanceOf(InsufficientPermissionException.class)
                .hasMessageContaining("관리자 권한이 필요합니다");
    }

    private void authenticate(String... authorities) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                "user", "password", authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
