package com.sleekydz86.finsight.core.media.youtube.adapter.requester;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.board.domain.Board;
import com.sleekydz86.finsight.core.global.exception.AiAnalysisFailedException;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeGeneratedContent;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeVideoMeta;
import com.sleekydz86.finsight.core.news.adapter.requester.overview.properties.OllamaProperties;
import com.sleekydz86.finsight.core.news.adapter.requester.overview.properties.OpenAiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class YoutubeAiContentRequester {

    private static final Logger log = LoggerFactory.getLogger(YoutubeAiContentRequester.class);
    private static final int SUMMARY_MAX_LENGTH = 1200;
    private static final int EDITOR_COMMENT_MAX_LENGTH = 2000;
    private static final int KEY_POINT_MAX_LENGTH = 300;

    private final WebClient webClient;
    private final WebClient ollamaWebClient;
    private final OpenAiProperties openAiProperties;
    private final OllamaProperties ollamaProperties;
    private final ObjectMapper objectMapper;
    private final String aiProvider;

    public YoutubeAiContentRequester(
            @Qualifier("webClient") WebClient webClient,
            @Qualifier("ollamaWebClient") WebClient ollamaWebClient,
            OpenAiProperties openAiProperties,
            OllamaProperties ollamaProperties,
            ObjectMapper objectMapper,
            @Value("${youtube.ai.provider:openai}") String aiProvider) {
        this.webClient = webClient;
        this.ollamaWebClient = ollamaWebClient;
        this.openAiProperties = openAiProperties;
        this.ollamaProperties = ollamaProperties;
        this.objectMapper = objectMapper;
        this.aiProvider = aiProvider == null ? "openai" : aiProvider.trim();
    }

    public YoutubeGeneratedContent generate(YoutubeVideoMeta videoMeta, Board board) {
        String prompt = buildPrompt(videoMeta, board);
        if (usesLocalLlama()) {
            return generateWithOllama(prompt);
        }
        RuntimeException lastFailure = null;
        if (openAiProperties.isConfigured()) {
            try {
                return readModelContent(callOpenAi(prompt), "openai");
            } catch (RuntimeException exception) {
                log.warn("게시글 {} OpenAI 보강 실패: {}", board.getId(), exception.getMessage());
                lastFailure = exception;
            }
        }
        if (ollamaProperties.isEnabled()) {
            try {
                return readModelContent(callOllama(prompt), "ollama");
            } catch (RuntimeException exception) {
                log.warn("게시글 {} Ollama 보강 실패: {}", board.getId(), exception.getMessage());
                lastFailure = exception;
            }
        }
        if (lastFailure != null) {
            throw new AiAnalysisFailedException(activeModelName(), "영상 보강 문장을 만들지 못했습니다.", lastFailure);
        }
        throw new AiAnalysisFailedException("NONE", "OpenAI 키 또는 Ollama가 설정되어 있지 않습니다.");
    }

    private boolean usesLocalLlama() {
        return "ollama".equalsIgnoreCase(aiProvider) || "llama".equalsIgnoreCase(aiProvider);
    }

    private YoutubeGeneratedContent generateWithOllama(String prompt) {
        if (!ollamaProperties.isEnabled()) {
            throw new AiAnalysisFailedException("ollama", "로컬 AI 보강은 Ollama가 켜져 있어야 합니다.");
        }
        try {
            return readModelContent(callOllama(prompt), "ollama");
        } catch (AiAnalysisFailedException exception) {
            throw exception;
        } catch (WebClientResponseException exception) {
            throw ollamaHttpFailure(exception);
        }
    }

    private AiAnalysisFailedException ollamaHttpFailure(WebClientResponseException exception) {
        String model = ollamaProperties.getModel();
        if (exception.getStatusCode().value() == 404) {
            return new AiAnalysisFailedException(
                    model,
                    "Ollama에 " + model + " 모델이 없습니다. ollama pull " + model + " 를 실행하세요.",
                    exception);
        }
        return new AiAnalysisFailedException(model, "Ollama 호출에 실패했습니다.", exception);
    }

    private String callOpenAi(String prompt) {
        Map<String, Object> responseBody = webClient.post()
                .uri(openAiProperties.getBaseUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + openAiProperties.getApiKey())
                .bodyValue(chatBody(openAiProperties.getModel(), prompt))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(Duration.ofSeconds(30))
                .block();
        if (responseBody == null) {
            throw new AiAnalysisFailedException(openAiProperties.getModel(), "OpenAI 응답이 비어 있습니다.");
        }
        return openAiContent(responseBody);
    }

    private String callOllama(String prompt) {
        String baseUrl = ollamaProperties.getBaseUrl() == null ? "" : ollamaProperties.getBaseUrl().replaceAll("/+$", "");
        Map<String, Object> responseBody = ollamaWebClient.post()
                .uri(baseUrl + "/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "model", ollamaProperties.getModel(),
                        "messages", List.of(
                                Map.of("role", "system", "content", "You are a Korean financial media editor. Reply with JSON only."),
                                Map.of("role", "user", "content", prompt)
                        ),
                        "stream", false,
                        "options", Map.of("temperature", 0.2, "num_ctx", 2048, "num_predict", 400)
                ))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(Duration.ofSeconds(Math.max(ollamaProperties.getTimeoutSeconds(), 30)))
                .block();
        if (responseBody == null) {
            throw new AiAnalysisFailedException(ollamaProperties.getModel(), "Ollama 응답이 비어 있습니다.");
        }
        return ollamaContent(responseBody);
    }

    private Map<String, Object> chatBody(String model, String prompt) {
        return Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a Korean financial media editor. Reply with JSON only."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.2
        );
    }

    private YoutubeGeneratedContent readModelContent(String content, String provider) {
        try {
            YoutubeAiResponse response = objectMapper.readValue(extractJsonObject(content), YoutubeAiResponse.class);
            return requireGenerated(response, provider);
        } catch (AiAnalysisFailedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AiAnalysisFailedException(provider, "보강 JSON을 읽지 못했습니다.", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private String openAiContent(Map<String, Object> responseBody) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new AiAnalysisFailedException(openAiProperties.getModel(), "OpenAI 응답 choices가 비어 있습니다.");
        }
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        if (message == null) {
            throw new AiAnalysisFailedException(openAiProperties.getModel(), "OpenAI 응답 message가 없습니다.");
        }
        String content = (String) message.get("content");
        if (content == null || content.isBlank()) {
            throw new AiAnalysisFailedException(openAiProperties.getModel(), "OpenAI 응답 content가 비어 있습니다.");
        }
        return content;
    }

    @SuppressWarnings("unchecked")
    private String ollamaContent(Map<String, Object> responseBody) {
        Map<String, Object> message = (Map<String, Object>) responseBody.get("message");
        if (message == null) {
            throw new AiAnalysisFailedException(ollamaProperties.getModel(), "Ollama 응답 message가 없습니다.");
        }
        String content = (String) message.get("content");
        if (content == null || content.isBlank()) {
            throw new AiAnalysisFailedException(ollamaProperties.getModel(), "Ollama 응답 content가 비어 있습니다.");
        }
        return content;
    }

    String buildPrompt(YoutubeVideoMeta videoMeta, Board board) {
        String title = defaultText(videoMeta.getYoutubeTitle(), board.getTitle());
        String category = defaultText(videoMeta.getCategory(), "금융 시장");
        String channelTitle = defaultText(videoMeta.getChannelTitle(), "알 수 없는 채널");

        return String.join("\n",
                "다음 유튜브 금융 영상의 제목, 채널, 카테고리만으로 편집 보조 데이터를 생성해 주세요.",
                "영상 설명문은 광고가 많아 입력하지 않습니다. 설명문을 추측하거나 인용하지 마세요.",
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
                "- 제목에 없는 수치, 종목, 사실은 단정하지 않습니다.",
                "",
                "입력 데이터:",
                "{",
                "  \"title\": \"" + escapeJson(title) + "\",",
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

    private YoutubeGeneratedContent requireGenerated(YoutubeAiResponse response, String provider) {
        String summary = trimToLength(normalizeText(response != null ? response.summary() : null), SUMMARY_MAX_LENGTH);
        String editorComment = trimToLength(normalizeText(response != null ? response.editorComment() : null), EDITOR_COMMENT_MAX_LENGTH);
        List<String> keyPoints = normalizeKeyPoints(response != null ? response.keyPoints() : null);
        if (summary == null || editorComment == null || keyPoints.size() < 3) {
            throw new AiAnalysisFailedException(provider, "요약, 편집 코멘트, 핵심 포인트 3개가 필요합니다.");
        }
        return YoutubeGeneratedContent.builder()
                .summary(summary)
                .editorComment(editorComment)
                .keyPoints(keyPoints)
                .build();
    }

    private String activeModelName() {
        if (openAiProperties.isConfigured()) {
            return openAiProperties.getModel();
        }
        if (ollamaProperties.isEnabled()) {
            return ollamaProperties.getModel();
        }
        return "NONE";
    }

    private List<String> normalizeKeyPoints(List<String> keyPoints) {
        Set<String> normalized = new LinkedHashSet<>();
        if (keyPoints != null) {
            for (String keyPoint : keyPoints) {
                String value = trimToLength(normalizeText(keyPoint), KEY_POINT_MAX_LENGTH);
                if (value != null) {
                    normalized.add(value);
                }
            }
        }
        return new ArrayList<>(normalized).stream().limit(3).toList();
    }

    private String extractJsonObject(String content) {
        String trimmed = stripCodeFence(content);
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new AiAnalysisFailedException(activeModelName(), "보강 응답에서 JSON 객체를 찾지 못했습니다.");
        }
        return trimmed.substring(start, end + 1);
    }

    private String stripCodeFence(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z0-9]*\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed.trim();
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
