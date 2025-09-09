package com.sleekydz86.finsight.core.global.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sleekydz86.finsight.core.global.exception.dto.ErrorResponse;
import com.sleekydz86.finsight.core.global.exception.dto.ValidationErrorResponse;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private List<String> errors;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    private String requestId;
    private int statusCode;

    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setData(data);
        response.setMessage("Success");
        response.setStatusCode(200);
        return response;
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setData(data);
        response.setMessage(message);
        response.setStatusCode(200);
        return response;
    }

    public static <T> ApiResponse<T> error(String message, int statusCode) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        response.setStatusCode(statusCode);
        return response;
    }

    public static <T> ApiResponse<T> error(String message, List<String> errors, int statusCode) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrors(errors);
        response.setStatusCode(statusCode);
        return response;
    }

    public static <T> ApiResponse<T> error(ErrorResponse errorResponse) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setData((T) errorResponse);
        response.setMessage(errorResponse.getMessage());
        response.setStatusCode(errorResponse.getStatus());
        return response;
    }

    public static <T> ApiResponse<T> error(ValidationErrorResponse errorResponse) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setData((T) errorResponse);
        response.setMessage(errorResponse.getMessage());
        response.setStatusCode(errorResponse.getStatus());
        return response;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        response.setStatusCode(400);
        return response;
    }

}