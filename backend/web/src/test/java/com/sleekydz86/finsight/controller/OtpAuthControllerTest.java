package com.sleekydz86.finsight.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.auth.dto.*;
import com.sleekydz86.finsight.core.auth.service.AuthenticationService;
import com.sleekydz86.finsight.core.auth.service.OtpAuthenticationService;
import com.sleekydz86.finsight.core.user.service.UserService;
import com.sleekydz86.finsight.web.controller.AuthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class OtpAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private UserService userService;

    @MockBean
    private OtpAuthenticationService otpAuthenticationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void OTP_설정_요청_테스트() throws Exception {
        OtpSetupRequest request = new OtpSetupRequest("test@example.com");
        OtpSetupResponse response = new OtpSetupResponse("secret", "qrCode", "message");

        when(otpAuthenticationService.setupOtp(any(OtpSetupRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/otp/setup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void OTP_검증_요청_테스트() throws Exception {
        OtpVerifyRequest request = new OtpVerifyRequest("test@example.com", "123456");
        OtpVerifyResponse response = new OtpVerifyResponse(true, "성공");

        when(otpAuthenticationService.verifyOtp(any(OtpVerifyRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/otp/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void OTP_로그인_요청_테스트() throws Exception {
        OtpLoginRequest request = new OtpLoginRequest("test@example.com", "password123!", "123456");

        when(otpAuthenticationService.loginWithOtp(any(OtpLoginRequest.class)))
                .thenReturn(com.sleekydz86.finsight.core.global.dto.ApiResponse.success(null, "성공"));

        mockMvc.perform(post("/api/v1/auth/login/otp")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}