package com.sleekydz86.finsight.core.global.exception;

public class AiAnalysisFailedException extends BaseException {
    private final String model;
    private final String reason;

    public AiAnalysisFailedException(String model, String reason) {
        super("AI 분석에 실패했습니다. 모델: " + model + ", 사유: " + reason,
                "NEWS_003", "AI Analysis Failed", 500);
        this.model = model;
        this.reason = reason;
    }

    public String getModel() {
        return model;
    }

    public String getReason() {
        return reason;
    }
}