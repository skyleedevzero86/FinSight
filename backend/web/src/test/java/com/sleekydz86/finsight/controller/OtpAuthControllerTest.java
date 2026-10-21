package com.sleekydz86.finsight.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.WebApplication;
import com.sleekydz86.finsight.core.auth.dto.*;
import com.sleekydz86.finsight.core.auth.service.AuthenticationService;
import com.sleekydz86.finsight.core.auth.service.OtpAuthenticationService;
import com.sleekydz86.finsight.core.user.service.UserService;
import com.sleekydz86.finsight.web.controller.AuthController;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@ContextConfiguration(classes = WebApplication.class)
@Disabled("로컬 MySQL이 없으면 웹 컨텍스트가 뜨지 않음. 통합 테스트 환경 정리 예정")
class OtpAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private UserService userService;

    @MockBean
    private OtpAuthenticationService otpAuthenticationService;

    @MockBean
    private com.sleekydz86.finsight.core.auth.service.SocialAuthService socialAuthService;

    @MockBean
    private com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort userPersistencePort;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("OTP 설정 요청 테스트")
    @WithMockUser
    void otpSetupRequest() throws Exception {
        // given
        OtpSetupRequest request = new OtpSetupRequest("test@example.com");
        OtpSetupResponse response = new OtpSetupResponse("secret", "qrCode", "message");
        when(otpAuthenticationService.setupOtp(any(OtpSetupRequest.class)))
                .thenReturn(response);

        // when

        // then
        mockMvc.perform(post("/api/v1/auth/otp/setup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("OTP 검증 요청 테스트")
    @WithMockUser
    void otpVerifyRequest() throws Exception {
        // given
        OtpVerifyRequest request = new OtpVerifyRequest("test@example.com", "123456");
        OtpVerifyResponse response = new OtpVerifyResponse(true, "성공");
        when(otpAuthenticationService.verifyOtp(any(OtpVerifyRequest.class)))
                .thenReturn(response);

        // when

        // then
        mockMvc.perform(post("/api/v1/auth/otp/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("OTP 로그인 요청 테스트")
    @WithMockUser
    void otpLoginRequest() throws Exception {
        // given
        OtpLoginRequest request = new OtpLoginRequest("test@example.com", "password123!", "123456");
        when(otpAuthenticationService.loginWithOtp(any(OtpLoginRequest.class)))
                .thenReturn(com.sleekydz86.finsight.core.global.dto.ApiResponse.success(null, "성공"));

        // when

        // then
        mockMvc.perform(post("/api/v1/auth/login/otp")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
