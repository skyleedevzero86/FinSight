package com.sleekydz86.finsight.core.news.domain.vo;

import java.util.Objects;

public class Content {
    private final String title;
    private final String content;

    public Content() {
        this.title = null;
        this.content = null;
    }

    public Content(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Content content1 = (Content) o;
        return Objects.equals(title, content1.title) &&
                Objects.equals(content, content1.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, content);
    }

    @Override
    public String toString() {
        return "Content{" +
                "title='" + title + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}