package com.sleekydz86.finsight.core.config;

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
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 보안_헤더가_올바르게_설정되어야_한다() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-XSS-Protection", "1; mode=block"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Strict-Transport-Security",
                        "max-age=31536000; includeSubDomains; preload"));
    }

    @Test
    void 인증되지_않은_사용자는_보호된_리소스에_접근할_수_없어야_한다() throws Exception {
        mockMvc.perform(get("/news/search"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void 인증된_사용자는_보호된_리소스에_접근할_수_있어야_한다() throws Exception {
        mockMvc.perform(get("/news/search"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void 일반_사용자는_관리자_리소스에_접근할_수_없어야_한다() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 관리자는_관리자_리소스에_접근할_수_있어야_한다() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    void 공개_엔드포인트는_인증_없이_접근할_수_있어야_한다() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/login"))
                .andExpect(status().isBadRequest());
    }
}