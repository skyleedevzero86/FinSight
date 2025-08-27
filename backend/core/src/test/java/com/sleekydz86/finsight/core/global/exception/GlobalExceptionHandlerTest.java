package com.sleekydz86.finsight.core.global.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Locale;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;
    private GlobalExceptionHandler exceptionHandler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage("test.message", Locale.getDefault(), "Test message");
        messageSource.addMessage("error.generic", Locale.getDefault(), "Internal server error occurred");
        messageSource.addMessage("error.authentication.failed", Locale.getDefault(), "Authentication failed");
        messageSource.addMessage("error.validation.failed", Locale.getDefault(), "Validation failed");

        objectMapper = new ObjectMapper();
        exceptionHandler = new GlobalExceptionHandler(messageSource);

        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(exceptionHandler)
                .build();
    }

    @Test
    void 기본_예외_처리_테스트() throws Exception {
        mockMvc.perform(post("/test/exception")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void 검증_예외_처리_테스트() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"age\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void 인증_실패_예외_처리_테스트() throws Exception {
        mockMvc.perform(post("/test/auth-failed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @PostMapping("/exception")
        public void throwException() {
            throw new RuntimeException("Test exception");
        }

        @PostMapping("/validation")
        public void validateRequest(@Valid @RequestBody TestRequest request) {
        }

        @PostMapping("/auth-failed")
        public void throwAuthException() {
            throw new AuthenticationFailedException("Invalid credentials");
        }
    }

    static class TestRequest {
        @NotBlank(message = "Name is required")
        private String name;

        @NotNull(message = "Age is required")
        private Integer age;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
    }
}