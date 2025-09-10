package com.sleekydz86.finsight.core.global.dto;

import org.springframework.data.domain.Page;
import java.util.List;

public class PaginationResponse<T> {
    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean first;
    private final boolean last;
    private final boolean hasNext;
    private final boolean hasPrevious;

    public PaginationResponse() {
        this.content = List.of();
        this.page = 0;
        this.size = 0;
        this.totalElements = 0;
        this.totalPages = 0;
        this.first = true;
        this.last = true;
        this.hasNext = false;
        this.hasPrevious = false;
    }

    public PaginationResponse(List<T> content, int page, int size, long totalElements) {
        this.content = content != null ? content : List.of();
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = (int) Math.ceil((double) totalElements / size);
        this.first = page == 0;
        this.last = page >= totalPages - 1;
        this.hasNext = page < totalPages - 1;
        this.hasPrevious = page > 0;
    }

    public static <T> PaginationResponse<T> from(Page<T> page) {
        return new PaginationResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements());
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T> {
        private List<T> content = List.of();
        private int page = 0;
        private int size = 20;
        private long totalElements = 0;

        public Builder<T> content(List<T> content) {
            this.content = content != null ? content : List.of();
            return this;
        }

        public Builder<T> page(int page) {
            this.page = page;
            return this;
        }

        public Builder<T> size(int size) {
            this.size = size;
            return this;
        }

        public Builder<T> totalElements(long totalElements) {
            this.totalElements = totalElements;
            return this;
        }

        public PaginationResponse<T> build() {
            return new PaginationResponse<>(content, page, size, totalElements);
        }
    }

    public List<T> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isFirst() {
        return first;
    }

    public boolean isLast() {
        return last;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }

    @Override
    public String toString() {
        return "PaginationResponse{" +
                "content=" + content +
                ", page=" + page +
                ", size=" + size +
                ", totalElements=" + totalElements +
                ", totalPages=" + totalPages +
                ", first=" + first +
                ", last=" + last +
                ", hasNext=" + hasNext +
                ", hasPrevious=" + hasPrevious +
                '}';
    }
}