package com.sleekydz86.finsight.core.global.exception.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private final String errorCode;
    private final String errorType;
    private final String message;
    private final String userMessage;
    private final int httpStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime timestamp;

    private final String path;
    private final String method;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<String> validationErrors;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final Map<String, Object> additionalInfo;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String traceId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String correlationId;
}