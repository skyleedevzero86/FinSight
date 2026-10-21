package com.sleekydz86.finsight.web.mcp.tools;

import com.sleekydz86.finsight.core.health.domain.Health;
import com.sleekydz86.finsight.core.health.domain.port.in.HealthQueryUseCase;
import com.sleekydz86.finsight.core.health.domain.vo.HealthStatus;
import com.sleekydz86.finsight.core.health.domain.vo.SystemMetrics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminMcpToolsTest {

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("서버 상태는 관리자만 조회한다")
    void getServerHealth_requiresAdmin() {
        // when
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "user@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        // given
        AdminMcpTools tools = tools(new FixedHealthQuery("UP", "정상"));

        // then
        assertThatThrownBy(tools::getServerHealth)
                .hasMessageContaining("관리자 권한이 필요합니다");
    }

    @Test
    @DisplayName("관리자에게는 서버 상태를 반환한다")
    void getServerHealth_returnsStatusForAdmin() {
        // when
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        // given
        AdminMcpTools tools = tools(new FixedHealthQuery("UP", "정상"));

        // then
        assertThat(tools.getServerHealth()).isEqualTo(new AdminMcpTools.HealthItem("UP", "정상"));
    }

    @Test
    @DisplayName("내 즐겨찾기는 로그인이 필요하다")
    void listMyFavorites_requiresLogin() {
        // given
        AccountMcpTools tools = new AccountMcpTools(null, null, null);

        // when

        // then
        assertThatThrownBy(() -> tools.listMyFavorites(null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("로그인이 필요합니다.");
    }

    private static AdminMcpTools tools(HealthQueryUseCase healthQueryUseCase) {
        return new AdminMcpTools(null, healthQueryUseCase, null, null, null, null, null, null, null, null);
    }

    private static final class FixedHealthQuery implements HealthQueryUseCase {
        private final HealthStatus overall;

        private FixedHealthQuery(String status, String message) {
            this.overall = new HealthStatus(status, message);
        }

        @Override
        public HealthStatus getOverallHealth() {
            return overall;
        }

        @Override
        public Map<String, Object> getDetailedHealth() {
            throw unused();
        }

        @Override
        public SystemMetrics getSystemMetrics() {
            throw unused();
        }

        @Override
        public HealthStatus getDatabaseHealth() {
            throw unused();
        }

        @Override
        public HealthStatus getRedisHealth() {
            throw unused();
        }

        @Override
        public Map<String, HealthStatus> getExternalApisHealth() {
            throw unused();
        }

        @Override
        public Health getCompleteHealth() {
            throw unused();
        }

        private static UnsupportedOperationException unused() {
            return new UnsupportedOperationException("이 테스트는 해당 조회를 사용하지 않습니다.");
        }
    }
}
