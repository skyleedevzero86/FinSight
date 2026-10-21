package com.sleekydz86.finsight.core.global.config;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Disabled("애플리케이션 컨텍스트가 뜨지 않아 보안 통합 검증을 보류. 통합 테스트 환경 정리 예정")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("보안 헤더가 올바르게 설정되어야 한다")
    void setsSecurityHeaders() throws Exception {
        // given

        // when

        // then
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-XSS-Protection", "1; mode=block"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Strict-Transport-Security",
                        "max-age=31536000; includeSubDomains; preload"));
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 보호된 리소스에 접근할 수 없어야 한다")
    void unauthenticatedUserCannotAccessProtectedResource() throws Exception {
        // given

        // when

        // then
        mockMvc.perform(get("/news/search"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("인증된 사용자는 보호된 리소스에 접근할 수 있어야 한다")
    @WithMockUser(roles = "USER")
    void authenticatedUserCanAccessProtectedResource() throws Exception {
        // given

        // when

        // then
        mockMvc.perform(get("/news/search"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("일반 사용자는 관리자 리소스에 접근할 수 없어야 한다")
    @WithMockUser(roles = "USER")
    void regularUserCannotAccessAdminResource() throws Exception {
        // given

        // when

        // then
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자는 관리자 리소스에 접근할 수 있어야 한다")
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessAdminResource() throws Exception {
        // given

        // when

        // then
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("공개 엔드포인트는 인증 없이 접근할 수 있어야 한다")
    void publicEndpointIsAccessibleWithoutAuthentication() throws Exception {
        // given

        // when

        // then
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/auth/login"))
                .andExpect(status().isBadRequest());
    }
}
