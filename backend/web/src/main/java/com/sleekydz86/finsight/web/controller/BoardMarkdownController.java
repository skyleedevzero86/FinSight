package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.board.domain.port.in.dto.MarkdownRenderRequest;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.MarkdownRenderResponse;
import com.sleekydz86.finsight.core.board.markdown.MarkdownRenderResult;
import com.sleekydz86.finsight.core.board.markdown.MarkdownRenderingService;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "게시판 마크다운", description = "게시글 마크다운 렌더링 API")
@RestController
@RequestMapping("/api/v1/boards/markdown")
public class BoardMarkdownController {

    private final MarkdownRenderingService markdownRenderingService;

    public BoardMarkdownController(MarkdownRenderingService markdownRenderingService) {
        this.markdownRenderingService = markdownRenderingService;
    }

    @Operation(summary = "마크다운 렌더링", description = "마크다운 텍스트를 HTML로 렌더링합니다.")
    @PostMapping("/render")
    public ResponseEntity<ApiResponse<MarkdownRenderResponse>> render(@Valid @RequestBody MarkdownRenderRequest request) {
        MarkdownRenderResult result = markdownRenderingService.render(request.getMarkdown());
        MarkdownRenderResponse body = new MarkdownRenderResponse(result.sanitizedHtml(), result.plainText());
        return ResponseEntity.ok(ApiResponse.success(body, "마크다운 렌더링이 완료되었습니다"));
    }
}
