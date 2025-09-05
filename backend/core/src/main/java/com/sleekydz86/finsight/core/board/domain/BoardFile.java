package com.sleekydz86.finsight.core.board.domain;

import java.time.LocalDateTime;

public class BoardFile {
    private final Long id;
    private final Long boardId;
    private final String originalFileName;
    private final String storedFileName;
    private final String filePath;
    private final String contentType;
    private final Long fileSize;
    private final LocalDateTime uploadedAt;

    public BoardFile() {
        this.id = null;
        this.boardId = null;
        this.originalFileName = "";
        this.storedFileName = "";
        this.filePath = "";
        this.contentType = "";
        this.fileSize = 0L;
        this.uploadedAt = LocalDateTime.now();
    }

    public BoardFile(Long id, Long boardId, String originalFileName, String storedFileName,
                     String filePath, String contentType, Long fileSize, LocalDateTime uploadedAt) {
        this.id = id;
        this.boardId = boardId;
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.filePath = filePath;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.uploadedAt = uploadedAt;
    }

    public Long getId() { return id; }
    public Long getBoardId() { return boardId; }
    public String getOriginalFileName() { return originalFileName; }
    public String getStoredFileName() { return storedFileName; }
    public String getFilePath() { return filePath; }
    public String getContentType() { return contentType; }
    public Long getFileSize() { return fileSize; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BoardFile boardFile = (BoardFile) o;
        return id != null && id.equals(boardFile.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "BoardFile{" +
                "id=" + id +
                ", boardId=" + boardId +
                ", originalFileName='" + originalFileName + '\'' +
                ", storedFileName='" + storedFileName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", contentType='" + contentType + '\'' +
                ", fileSize=" + fileSize +
                ", uploadedAt=" + uploadedAt +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long boardId;
        private String originalFileName;
        private String storedFileName;
        private String filePath;
        private String contentType;
        private Long fileSize;
        private LocalDateTime uploadedAt = LocalDateTime.now();

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder boardId(Long boardId) {
            this.boardId = boardId;
            return this;
        }

        public Builder originalFileName(String originalFileName) {
            this.originalFileName = originalFileName;
            return this;
        }

        public Builder storedFileName(String storedFileName) {
            this.storedFileName = storedFileName;
            return this;
        }

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder fileSize(Long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public Builder uploadedAt(LocalDateTime uploadedAt) {
            this.uploadedAt = uploadedAt;
            return this;
        }

        public BoardFile build() {
            return new BoardFile(id, boardId, originalFileName, storedFileName,
                    filePath, contentType, fileSize, uploadedAt);
        }
    }
}