package com.sleekydz86.finsight.core.user.service;

import java.nio.file.Path;

public class StoredProfileImage {

    private final Path path;
    private final String contentType;

    public StoredProfileImage(Path path, String contentType) {
        this.path = path;
        this.contentType = contentType;
    }

    public Path getPath() {
        return path;
    }

    public String getContentType() {
        return contentType;
    }
}
