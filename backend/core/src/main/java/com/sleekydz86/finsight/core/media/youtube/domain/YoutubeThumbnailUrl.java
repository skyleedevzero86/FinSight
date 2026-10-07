package com.sleekydz86.finsight.core.media.youtube.domain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class YoutubeThumbnailUrl {

    private static final Pattern VIDEO_ID = Pattern.compile("^[A-Za-z0-9_-]{11}$");
    private static final Pattern YTIMG_ID = Pattern.compile("i\\.ytimg\\.com/vi/([^/?#]+)/", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNRELIABLE_SIZE = Pattern.compile("/(hqdefault|sddefault|maxresdefault)\\.", Pattern.CASE_INSENSITIVE);

    private YoutubeThumbnailUrl() {
    }

    public static boolean isVideoId(String videoId) {
        return videoId != null && VIDEO_ID.matcher(videoId.trim()).matches();
    }

    public static String displayUrl(String videoId, String storedUrl) {
        String stored = storedUrl == null ? "" : storedUrl.trim();
        String id = resolveVideoId(videoId, stored);
        if (!VIDEO_ID.matcher(id).matches()) {
            return null;
        }
        if (stored.startsWith("https://") && !UNRELIABLE_SIZE.matcher(stored).find()) {
            return stored;
        }
        return "https://i.ytimg.com/vi/" + id + "/mqdefault.jpg";
    }

    private static String resolveVideoId(String videoId, String storedUrl) {
        Matcher matcher = YTIMG_ID.matcher(storedUrl);
        if (matcher.find() && matcher.group(1) != null && !matcher.group(1).isBlank()) {
            return matcher.group(1).trim();
        }
        return videoId == null ? "" : videoId.trim();
    }
}
