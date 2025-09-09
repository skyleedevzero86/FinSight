package com.sleekydz86.finsight.core.global.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;

    private String sort;
    private String direction;

    public static PageRequest of(int page, int size) {
        return PageRequest.builder()
                .page(page)
                .size(size)
                .build();
    }

    public static PageRequest of(int page, int size, String sort, String direction) {
        return PageRequest.builder()
                .page(page)
                .size(size)
                .sort(sort)
                .direction(direction)
                .build();
    }

    public Pageable toPageable() {
        if (sort != null && !sort.isEmpty()) {
            Sort.Direction sortDirection = Sort.Direction.ASC;
            if (direction != null && direction.equalsIgnoreCase("desc")) {
                sortDirection = Sort.Direction.DESC;
            }
            return org.springframework.data.domain.PageRequest.of(page, size, Sort.by(sortDirection, sort));
        }
        return org.springframework.data.domain.PageRequest.of(page, size);
    }
}