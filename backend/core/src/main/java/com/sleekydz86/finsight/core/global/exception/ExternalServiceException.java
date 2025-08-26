package com.sleekydz86.finsight.core.global.exception;

public class ExternalServiceException extends BaseException {
    private final String serviceName;
    private final String endpoint;

    public ExternalServiceException(String serviceName, String endpoint, String reason) {
        super("외부 서비스 호출에 실패했습니다. 서비스: " + serviceName + ", 엔드포인트: " + endpoint + ", 사유: " + reason,
                "SYS_001", "External Service Error", 503);
        this.serviceName = serviceName;
        this.endpoint = endpoint;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getEndpoint() {
        return endpoint;
    }
}