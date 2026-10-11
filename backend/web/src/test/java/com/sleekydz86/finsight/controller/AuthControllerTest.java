package com.sleekydz86.finsight.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.WebApplication;
import com.sleekydz86.finsight.core.auth.dto.LoginRequest;
import com.sleekydz86.finsight.core.auth.service.AuthenticationService;
import com.sleekydz86.finsight.core.user.service.UserService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@ContextConfiguration(classes = WebApplication.class)
@Disabled("로컬 MySQL이 없으면 웹 컨텍스트가 뜨지 않음. 통합 테스트 환경 정리 예정")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private UserService userService;

    @MockBean
    private com.sleekydz86.finsight.core.auth.service.SocialAuthService socialAuthService;

    @MockBean
    private com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort userPersistencePort;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void 로그인_요청_테스트() throws Exception {
        LoginRequest request = new LoginRequest("test@example.com", "password123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
