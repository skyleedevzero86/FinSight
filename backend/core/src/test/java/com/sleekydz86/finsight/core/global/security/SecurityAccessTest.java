package com.sleekydz86.finsight.core.global.security;

import com.sleekydz86.finsight.core.global.exception.InsufficientPermissionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("관리자와 매니저 권한을 허용한다")
    void adminAndManagerAuthoritiesAreAccepted() {
        // given

        // when
        authenticate("ROLE_ADMIN");

        // then
        assertThat(SecurityAccess.isAdminOrManager()).isTrue();

        // when
        assertThatCode(SecurityAccess::requireAdminOrManager).doesNotThrowAnyException();
        authenticate("ROLE_MANAGER");

        // then
        assertThat(SecurityAccess.isAdminOrManager()).isTrue();

        // when
        assertThatCode(SecurityAccess::requireAdminOrManager).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("권한 일치는 정확해야 하고 비슷한 이름은 거절한다")
    void roleMatchIsExactAndDoesNotAcceptPlainOrLookalikeAuthorities() {
        // given

        // when
        authenticate("ADMIN", "ROLE_ADMINISTRATOR", "SCOPE_ROLE_MANAGER");

        // then
        assertThat(SecurityAccess.isAdminOrManager()).isFalse();
    }

    @Test
    @DisplayName("인증되지 않은 토큰은 관리자 권한이 있어도 거절한다")
    void unauthenticatedTokenIsRejectedEvenWithAdminAuthority() {
        // given
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                "user", "password", "ROLE_ADMIN");

        // when
        authentication.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // then
        assertThat(SecurityAccess.isAdminOrManager()).isFalse();
    }

    @Test
    @DisplayName("인증 정보가 없으면 거절한다")
    void missingAuthenticationIsRejected() {
        // given

        // when

        // then
        assertThat(SecurityAccess.isAdminOrManager()).isFalse();
    }

    @Test
    @DisplayName("일반 사용자에게는 관리자 또는 매니저 예외를 던진다")
    void requireAdminOrManagerThrowsDomainExceptionForRegularUser() {
        // given

        // when
        authenticate("ROLE_USER");

        // then
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
