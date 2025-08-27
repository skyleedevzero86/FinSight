package com.sleekydz86.finsight.core.global.exception;

public class AiAnalysisFailedException extends BaseException {
    private final String modelName;

    public AiAnalysisFailedException(String modelName, String message) {
        super("AI 분석에 실패했습니다: " + message,
                "AI_ANALYSIS_FAILED",
                "AI Analysis Error",
                500);
        this.modelName = modelName;
    }

    public AiAnalysisFailedException(String modelName, String message, Throwable cause) {
        super("AI 분석에 실패했습니다: " + message,
                "AI_ANALYSIS_FAILED",
                "AI Analysis Error",
                500,
                cause);
        this.modelName = modelName;
    }

    public String getModelName() {
        return modelName;
    }
}