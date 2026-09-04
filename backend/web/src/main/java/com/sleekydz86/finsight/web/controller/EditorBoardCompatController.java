package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.board.domain.Board;
import com.sleekydz86.finsight.core.board.domain.BoardStatus;
import com.sleekydz86.finsight.core.board.domain.BoardType;
import com.sleekydz86.finsight.core.board.domain.port.BoardQueryUseCase;
import com.sleekydz86.finsight.core.board.domain.port.in.BoardCommandUseCase;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardCreateRequest;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardDetailResponse;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardListResponse;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardNavigationResponse;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardUpdateRequest;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.MarkdownRenderRequest;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.MarkdownRenderResponse;
import com.sleekydz86.finsight.core.board.markdown.MarkdownRenderingService;
import com.sleekydz86.finsight.core.global.annotation.CurrentUser;
import com.sleekydz86.finsight.core.global.dto.AuthenticatedUser;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.global.exception.ValidationException;
import com.sleekydz86.finsight.web.dto.editor.EditorBootstrapResponse;
import com.sleekydz86.finsight.web.dto.editor.EditorDocumentPageResponse;
import com.sleekydz86.finsight.web.dto.editor.EditorDocumentSummaryResponse;
import com.sleekydz86.finsight.web.dto.editor.EditorDraftResponse;
import com.sleekydz86.finsight.web.dto.editor.EditorSaveDraftRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@RestController
@RequestMapping("/api/editor")
public class EditorBoardCompatController {

    private final BoardQueryUseCase boardQueryUseCase;
    private final BoardCommandUseCase boardCommandUseCase;
    private final MarkdownRenderingService markdownRenderingService;

    public EditorBoardCompatController(
            BoardQueryUseCase boardQueryUseCase,
            BoardCommandUseCase boardCommandUseCase,
            MarkdownRenderingService markdownRenderingService) {
        this.boardQueryUseCase = boardQueryUseCase;
        this.boardCommandUseCase = boardCommandUseCase;
        this.markdownRenderingService = markdownRenderingService;
    }

    @GetMapping("/bootstrap")
    public EditorBootstrapResponse loadBootstrap() {
        List<BoardListResponse> latest = boardQueryUseCase.getLatestBoards(15);
        List<String> tags = latest.stream()
                .flatMap(b -> Objects.requireNonNullElse(b.getHashtags(), List.<String>of()).stream())
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .limit(12)
                .toList();
        EditorDraftResponse empty = new EditorDraftResponse(
                null, "", "", "", List.of(), "", "", "", "", "", "draft", Instant.now(), null, null);
        return new EditorBootstrapResponse(empty, tags, null);
    }

    @PostMapping("/markdown/render")
    public MarkdownRenderResponse renderMarkdown(@Valid @RequestBody MarkdownRenderRequest request) {
        var r = markdownRenderingService.render(request.getMarkdown());
        return new MarkdownRenderResponse(r.sanitizedHtml(), r.plainText());
    }

    @GetMapping("/documents")
    public EditorDocumentPageResponse listDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "published") String status) {
        BoardStatus boardStatus = mapListStatusFilter(status);
        PaginationResponse<BoardListResponse> p =
                boardQueryUseCase.getEditorDocuments(BoardType.COMMUNITY, boardStatus, query, page, size);
        List<EditorDocumentSummaryResponse> content = p.getContent().stream().map(this::toSummary).toList();
        return new EditorDocumentPageResponse(
                content,
                p.getPage(),
                p.getSize(),
                p.getTotalElements(),
                p.getTotalPages(),
                p.isFirst(),
                p.isLast(),
                query,
                status);
    }

    @GetMapping("/documents/slug/{urlSlug}")
    public EditorDraftResponse loadPublishedDocumentByUrlSlug(@PathVariable String urlSlug) {
        if (urlSlug != null && urlSlug.matches("\\d+")) {
            return loadDocument(Long.parseLong(urlSlug));
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/documents/{documentId}")
    public EditorDraftResponse loadDocument(@PathVariable long documentId) {
        BoardDetailResponse d = boardQueryUseCase.getBoardDetailWithNavigation(documentId, BoardType.COMMUNITY, false);
        return toDraft(d);
    }

    @PostMapping("/draft")
    @ResponseStatus(HttpStatus.CREATED)
    public EditorDraftResponse saveDraft(
            @Valid @RequestBody EditorSaveDraftRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        if (!currentUser.getEmail().equalsIgnoreCase(request.author().trim())) {
            throw new ValidationException(
                    "작성자 정보가 로그인 사용자와 일치하지 않습니다",
                    List.of("작성자 정보는 인증 사용자와 일치해야 합니다"));
        }
        BoardStatus targetStatus = mapSaveStatus(request.status());
        List<String> tagList = Objects.requireNonNullElse(request.tags(), List.of());
        if (request.documentId() == null || request.documentId().isBlank()) {
            BoardCreateRequest createReq = new BoardCreateRequest(
                    request.title(), request.markdown(), BoardType.COMMUNITY, tagList, targetStatus);
            Board saved = boardCommandUseCase.createBoard(currentUser.getEmail(), createReq);
            BoardDetailResponse detail =
                    boardQueryUseCase.getBoardDetailWithNavigation(saved.getId(), BoardType.COMMUNITY, false);
            return mergeClientDraftFields(request, toDraft(detail));
        }
        long id = parseDocumentId(request.documentId());
        BoardUpdateRequest updateReq = new BoardUpdateRequest(
                request.title(), request.markdown(), tagList, targetStatus);
        boardCommandUseCase.updateBoard(currentUser.getEmail(), currentUser.getRole(), id, updateReq);
        BoardDetailResponse detail = boardQueryUseCase.getBoardDetailWithNavigation(id, BoardType.COMMUNITY, false);
        return mergeClientDraftFields(request, toDraft(detail));
    }

    @DeleteMapping("/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(
            @PathVariable long documentId, @CurrentUser AuthenticatedUser currentUser) {
        boardCommandUseCase.deleteBoard(currentUser.getEmail(), currentUser.getRole(), documentId);
    }

    private BoardStatus mapListStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return BoardStatus.ACTIVE;
        }
        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "draft" -> BoardStatus.DRAFT;
            case "published" -> BoardStatus.ACTIVE;
            default -> BoardStatus.ACTIVE;
        };
    }

    private BoardStatus mapSaveStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return BoardStatus.DRAFT;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "draft" -> BoardStatus.DRAFT;
            case "published" -> BoardStatus.ACTIVE;
            default -> BoardStatus.DRAFT;
        };
    }

    private String statusToEditor(BoardStatus s) {
        return switch (s) {
            case DRAFT -> "draft";
            case ACTIVE -> "published";
            default -> s.name().toLowerCase(Locale.ROOT);
        };
    }

    private EditorDocumentSummaryResponse toSummary(BoardListResponse b) {
        return new EditorDocumentSummaryResponse(
                String.valueOf(b.getId()),
                b.getTitle(),
                b.getAuthorEmail(),
                "",
                "",
                Objects.requireNonNullElse(b.getHashtags(), List.of()),
                statusToEditor(b.getStatus()),
                b.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant());
    }

    private EditorDraftResponse toDraft(BoardDetailResponse d) {
        EditorDocumentSummaryResponse prev = navToSummary(
                d.getNavigation() != null ? d.getNavigation().getPrevious() : null);
        EditorDocumentSummaryResponse next = navToSummary(
                d.getNavigation() != null ? d.getNavigation().getNext() : null);
        String intro = !d.getPlainTextPreview().isBlank()
                ? excerpt(d.getPlainTextPreview(), 180)
                : excerpt(d.getContent(), 180);
        return new EditorDraftResponse(
                String.valueOf(d.getId()),
                d.getTitle(),
                d.getAuthorEmail(),
                d.getContent(),
                Objects.requireNonNullElse(d.getHashtags(), List.of()),
                "",
                intro,
                "",
                d.getRenderedHtml(),
                d.getPlainTextPreview(),
                statusToEditor(d.getStatus()),
                d.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant(),
                prev,
                next);
    }

    private EditorDocumentSummaryResponse navToSummary(BoardNavigationResponse.BoardNavigationItem item) {
        if (item == null) {
            return null;
        }
        Instant updated;
        try {
            updated = LocalDateTime.parse(item.getCreatedAt()).atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception e) {
            updated = Instant.now();
        }
        return new EditorDocumentSummaryResponse(
                String.valueOf(item.getId()),
                item.getTitle(),
                item.getAuthorEmail(),
                "",
                "",
                List.of(),
                "published",
                updated);
    }

    private static String excerpt(String content, int max) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        if (content.length() <= max) {
            return content;
        }
        return content.substring(0, max);
    }

    private static long parseDocumentId(String documentId) {
        try {
            return Long.parseLong(documentId.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("documentId 형식이 올바르지 않습니다", List.of("documentId는 숫자여야 합니다"));
        }
    }

    private static EditorDraftResponse mergeClientDraftFields(EditorSaveDraftRequest req, EditorDraftResponse base) {
        String intro = req.introduction() != null && !req.introduction().isBlank()
                ? req.introduction()
                : base.introduction();
        String slug = req.urlSlug() != null && !req.urlSlug().isBlank() ? req.urlSlug() : base.urlSlug();
        String thumb = req.thumbnailImageUrl() != null && !req.thumbnailImageUrl().isBlank()
                ? req.thumbnailImageUrl()
                : base.thumbnailImageUrl();
        return new EditorDraftResponse(
                base.documentId(),
                base.title(),
                base.author(),
                base.markdown(),
                base.tags(),
                slug,
                intro,
                thumb,
                base.renderedHtml(),
                base.plainTextPreview(),
                base.status(),
                base.updatedAt(),
                base.previousDocument(),
                base.nextDocument());
    }
}
