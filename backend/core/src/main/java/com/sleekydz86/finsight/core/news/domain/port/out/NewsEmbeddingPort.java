package com.sleekydz86.finsight.core.news.domain.port.out;

public interface NewsEmbeddingPort {

    boolean isEnabled();

    boolean isReady();

    String modelName();

    int dimensions();

    float[] embed(String text);
}
