package com.sleekydz86.finsight.web.mcp;

public final class McpText {

    private McpText() {
    }

    public static String clip(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
