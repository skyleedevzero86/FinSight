package com.sleekydz86.finsight.core.board.domain.port.in.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MarkdownRenderRequest {

    @NotBlank(message = "markdown은 필수입니다")
    @Size(max = 100_000, message = "markdown은 100000자 이하여야 합니다")
    private String markdown;

    public MarkdownRenderRequest() {
    }

    public MarkdownRenderRequest(String markdown) {
        this.markdown = markdown;
    }

    public String getMarkdown() {
        return markdown;
    }

    public void setMarkdown(String markdown) {
        this.markdown = markdown;
    }
}
