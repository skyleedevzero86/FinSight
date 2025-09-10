package com.sleekydz86.finsight.core.news.domain.vo;

import java.util.Objects;

public class Content {
    private final String title;
    private final String content;
    private final String url;
    private final String imageUrl;
    private final String excerpt;

    public Content() {
        this.title = null;
        this.content = null;
        this.url = null;
        this.imageUrl = null;
        this.excerpt = null;
    }

    public Content(String title, String content) {
        this(title, content, null, null, null);
    }

    public Content(String title, String content, String url) {
        this(title, content, url, null, null);
    }

    public Content(String title, String content, String url, String imageUrl, String excerpt) {
        this.title = title;
        this.content = content;
        this.url = url;
        this.imageUrl = imageUrl;
        this.excerpt = excerpt;
    }

    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getUrl() { return url; }
    public String getImageUrl() { return imageUrl; }
    public String getExcerpt() { return excerpt; }

    public Content withUrl(String url) {
        return new Content(this.title, this.content, url, this.imageUrl, this.excerpt);
    }

    public Content withImageUrl(String imageUrl) {
        return new Content(this.title, this.content, this.url, imageUrl, this.excerpt);
    }

    public Content withExcerpt(String excerpt) {
        return new Content(this.title, this.content, this.url, this.imageUrl, excerpt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Content content1 = (Content) o;
        return Objects.equals(title, content1.title) &&
                Objects.equals(content, content1.content) &&
                Objects.equals(url, content1.url) &&
                Objects.equals(imageUrl, content1.imageUrl) &&
                Objects.equals(excerpt, content1.excerpt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, content, url, imageUrl, excerpt);
    }

    @Override
    public String toString() {
        return "Content{" +
                "title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", url='" + url + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", excerpt='" + excerpt + '\'' +
                '}';
    }
}