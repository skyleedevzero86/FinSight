package com.sleekydz86.finsight.core.auth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(JwtAccessDeniedHandler.class);

    private final ObjectMapper objectMapper;

    public JwtAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        String requestURI = request.getRequestURI();
        log.warn("권한이 없는 요청: {} {}", request.getMethod(), requestURI);

        if (requestURI.startsWith("/api/")) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            ApiResponse<Object> apiResponse = ApiResponse.error(
                    HttpStatus.FORBIDDEN,
                    "Access denied to this resource"
            );

            response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
        } else {
            response.sendRedirect("/");
        }
    }
}