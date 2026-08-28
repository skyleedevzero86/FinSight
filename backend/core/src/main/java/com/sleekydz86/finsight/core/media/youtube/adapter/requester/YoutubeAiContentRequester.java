package com.sleekydz86.finsight.core.media.youtube.adapter.requester;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.board.domain.Board;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeGeneratedContent;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeVideoMeta;
import com.sleekydz86.finsight.core.news.adapter.requester.overview.properties.OpenAiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class YoutubeAiContentRequester {

    private static final Logger log = LoggerFactory.getLogger(YoutubeAiContentRequester.class);
    private static final int SUMMARY_MAX_LENGTH = 1200;
    private static final int EDITOR_COMMENT_MAX_LENGTH = 2000;
    private static final int KEY_POINT_MAX_LENGTH = 300;

    private final WebClient webClient;
    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;

    public YoutubeAiContentRequester(
            WebClient webClient,
            OpenAiProperties openAiProperties,
            ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.openAiProperties = openAiProperties;
        this.objectMapper = objectMapper;
    }

    public YoutubeGeneratedContent generate(YoutubeVideoMeta videoMeta, Board board) {
        if (!isRemoteGenerationAvailable()) {
            return createFallbackContent(videoMeta, board);
        }

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", openAiProperties.getModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are a Korean financial media editor."),
                            Map.of("role", "user", "content", buildPrompt(videoMeta, board))
                    ),
                    "temperature", 0.2
            );

            Map<String, Object> responseBody = webClient.post()
                    .uri(openAiProperties.getBaseUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + openAiProperties.getApiKey())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (responseBody == null) {
                return createFallbackContent(videoMeta, board);
            }

            return parseResponse(responseBody, videoMeta, board);
        } catch (Exception e) {
            log.warn("게시글 {}에 대한 YouTube AI 콘텐츠 생성 실패: {}", board.getId(), e.getMessage());
            return createFallbackContent(videoMeta, board);
        }
    }

    private YoutubeGeneratedContent parseResponse(
            Map<String, Object> responseBody,
            YoutubeVideoMeta videoMeta,
            Board board) throws Exception {

        String content = extractContent(responseBody);
        String normalizedContent = stripCodeFence(content);
        YoutubeAiResponse response = objectMapper.readValue(normalizedContent, YoutubeAiResponse.class);
        return normalizeResponse(response, videoMeta, board);
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> responseBody) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("OpenAI 응답 choices가 비어 있습니다");
        }

        Map<String, Object> firstChoice = choices.get(0);
        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
        if (message == null) {
            throw new IllegalStateException("OpenAI 응답 message가 누락되었습니다");
        }

        String content = (String) message.get("content");
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("OpenAI 응답 content가 비어 있습니다");
        }

        return content;
    }

    private String buildPrompt(YoutubeVideoMeta videoMeta, Board board) {
        String title = defaultText(videoMeta.getYoutubeTitle(), board.getTitle());
        String description = defaultText(videoMeta.getYoutubeDescription(), board.getContent());
        String category = defaultText(videoMeta.getCategory(), "금융 시장");
        String channelTitle = defaultText(videoMeta.getChannelTitle(), "알 수 없는 채널");

        return String.join("\n",
                "다음 유튜브 금융 영상 메타데이터를 바탕으로 게시판 초안용 편집 보조 데이터를 생성해 주세요.",
                "",
                "반드시 JSON 객체만 반환해 주세요. 설명 문장, 마크다운, 코드블록은 금지입니다.",
                "",
                "필수 필드:",
                "- summary: 한국어 2~3문장 요약",
                "- editorComment: 한국어 편집자 코멘트 2~4문장",
                "- keyPoints: 한국어 핵심 포인트 배열 3개",
                "",
                "작성 규칙:",
                "- 과장된 표현을 피하고 금융 콘텐츠 편집자의 어조로 작성합니다.",
                "- summary는 사용자가 리스트 카드에서 읽는다고 가정하고 간결하게 작성합니다.",
                "- editorComment는 상세 페이지 본문 위에 들어갈 편집자 해설처럼 작성합니다.",
                "- keyPoints는 짧은 문장으로 작성하고 중복 없이 3개를 반환합니다.",
                "- 원문에 없는 사실은 단정하지 말고, 메타데이터 범위 안에서만 정리합니다.",
                "",
                "입력 데이터:",
                "{",
                "  \"title\": \"" + escapeJson(title) + "\",",
                "  \"description\": \"" + escapeJson(description) + "\",",
                "  \"category\": \"" + escapeJson(category) + "\",",
                "  \"channelTitle\": \"" + escapeJson(channelTitle) + "\"",
                "}",
                "",
                "반환 형식:",
                "{",
                "  \"summary\": \"...\",",
                "  \"editorComment\": \"...\",",
                "  \"keyPoints\": [\"...\", \"...\", \"...\"]",
                "}");
    }

    private YoutubeGeneratedContent normalizeResponse(
            YoutubeAiResponse response,
            YoutubeVideoMeta videoMeta,
            Board board) {

        YoutubeGeneratedContent fallback = createFallbackContent(videoMeta, board);

        String summary = trimToLength(defaultText(response != null ? response.summary() : null, fallback.getSummary()), SUMMARY_MAX_LENGTH);
        String editorComment = trimToLength(
                defaultText(response != null ? response.editorComment() : null, fallback.getEditorComment()),
                EDITOR_COMMENT_MAX_LENGTH);
        List<String> keyPoints = normalizeKeyPoints(
                response != null ? response.keyPoints() : null,
                fallback.getKeyPoints());

        return YoutubeGeneratedContent.builder()
                .summary(summary)
                .editorComment(editorComment)
                .keyPoints(keyPoints)
                .build();
    }

    private YoutubeGeneratedContent createFallbackContent(YoutubeVideoMeta videoMeta, Board board) {
        String title = defaultText(videoMeta.getYoutubeTitle(), board.getTitle());
        String description = defaultText(videoMeta.getYoutubeDescription(), board.getContent());
        String category = defaultText(videoMeta.getCategory(), "금융 시장");
        String channelTitle = defaultText(videoMeta.getChannelTitle(), "해당 채널");

        String summary = trimToLength(
                defaultText(extractPreview(description, 2),
                        title + "를 중심으로 " + category + " 흐름을 빠르게 파악할 수 있는 영상입니다."),
                SUMMARY_MAX_LENGTH);

        String editorComment = trimToLength(
                channelTitle + " 채널의 이 영상은 " + category + " 이슈를 짧은 시간 안에 훑어보기에 적합합니다. " +
                        "관리자는 이 초안을 바탕으로 본문과 태그를 다듬어 서비스형 콘텐츠로 발행하면 됩니다.",
                EDITOR_COMMENT_MAX_LENGTH);

        Set<String> points = new LinkedHashSet<>();
        points.add(trimToLength(title + "와 관련된 핵심 흐름을 먼저 확인할 수 있습니다.", KEY_POINT_MAX_LENGTH));
        points.add(trimToLength(category + " 관점에서 관련 뉴스와 함께 보면 이해하기 좋습니다.", KEY_POINT_MAX_LENGTH));
        if (description != null && !description.isBlank()) {
            points.add(trimToLength(extractPreview(description, 1), KEY_POINT_MAX_LENGTH));
        }
        points.add(trimToLength(channelTitle + " 채널 맥락까지 함께 참고할 수 있습니다.", KEY_POINT_MAX_LENGTH));

        List<String> keyPoints = points.stream()
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .limit(3)
                .toList();

        return YoutubeGeneratedContent.builder()
                .summary(summary)
                .editorComment(editorComment)
                .keyPoints(keyPoints)
                .build();
    }

    private List<String> normalizeKeyPoints(List<String> keyPoints, List<String> fallbackKeyPoints) {
        Set<String> normalized = new LinkedHashSet<>();
        if (keyPoints != null) {
            for (String keyPoint : keyPoints) {
                String value = trimToLength(normalizeText(keyPoint), KEY_POINT_MAX_LENGTH);
                if (value != null) {
                    normalized.add(value);
                }
            }
        }

        if (normalized.size() < 3 && fallbackKeyPoints != null) {
            for (String fallbackKeyPoint : fallbackKeyPoints) {
                String value = trimToLength(normalizeText(fallbackKeyPoint), KEY_POINT_MAX_LENGTH);
                if (value != null) {
                    normalized.add(value);
                }
                if (normalized.size() >= 3) {
                    break;
                }
            }
        }

        return new ArrayList<>(normalized).stream().limit(3).toList();
    }

    private String stripCodeFence(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z0-9]*\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed.trim();
    }

    private boolean isRemoteGenerationAvailable() {
        return hasText(openAiProperties.getBaseUrl())
                && hasText(openAiProperties.getApiKey())
                && hasText(openAiProperties.getModel())
                && !openAiProperties.getApiKey().contains("placeholder")
                && !openAiProperties.getApiKey().startsWith("local-");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultText(String value, String fallback) {
        String normalized = normalizeText(value);
        return normalized != null ? normalized : normalizeText(fallback);
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String extractPreview(String value, int sentenceLimit) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }

        String[] parts = normalized.split("(?<=[.!?。！？])\\s+");
        List<String> selected = new ArrayList<>();
        for (String part : parts) {
            String trimmed = normalizeText(part);
            if (trimmed != null) {
                selected.add(trimmed);
            }
            if (selected.size() >= sentenceLimit) {
                break;
            }
        }

        if (selected.isEmpty()) {
            return trimToLength(normalized, 220);
        }
        return String.join(" ", selected);
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record YoutubeAiResponse(
            String summary,
            String editorComment,
            List<String> keyPoints
    ) {
    }
}
