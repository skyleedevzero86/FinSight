package com.sleekydz86.finsight.core.global.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MessageSource messageSource;

    @Test
    void 사용자를_찾을_수_없을_때_적절한_에러_응답을_반환한다() throws Exception {

        mockMvc.perform(get("/test/user-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_001"))
                .andExpect(jsonPath("$.errorType").value("User Not Found"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.userMessage").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void 인증_실패시_적절한_에러_응답을_반환한다() throws Exception {

        mockMvc.perform(get("/test/authentication-failed"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_001"))
                .andExpect(jsonPath("$.errorType").value("Authentication Failed"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.userMessage").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());
    }
}