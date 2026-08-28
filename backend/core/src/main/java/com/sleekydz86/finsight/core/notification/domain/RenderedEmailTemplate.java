package com.sleekydz86.finsight.core.notification.domain;

import java.util.Map;

public record RenderedEmailTemplate(String subject, String htmlContent) {

    public static String apply(String source, Map<String, String> variables) {
        if (source == null) {
            return "";
        }
        String result = source;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace("{{" + key + "}}", value);
        }
        return result;
    }
}
