package com.sleekydz86.finsight.core.notification.service;

public final class EmailBodyPreviewSanitizer {

    private static final int BODY_PREVIEW_MAX = 2000;

    private EmailBodyPreviewSanitizer() {
    }

    public static String sanitize(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        String stripped = body
                .replaceAll("(?is)<style[^>]*>.*?</style>", " ")
                .replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("(?is)<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        stripped = stripped.replaceAll("\\b\\d{4,8}\\b", "******");
        if (stripped.length() > BODY_PREVIEW_MAX) {
            return stripped.substring(0, BODY_PREVIEW_MAX) + "...";
        }
        return stripped;
    }
}
