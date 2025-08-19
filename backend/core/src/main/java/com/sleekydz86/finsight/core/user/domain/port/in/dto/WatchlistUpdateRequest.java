package com.sleekydz86.finsight.core.user.domain.port.in.dto;

import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import java.util.List;
import java.util.Objects;

public class WatchlistUpdateRequest {
    private final List<TargetCategory> categories;

    public WatchlistUpdateRequest(List<TargetCategory> categories) {
        this.categories = categories;
    }

    public List<TargetCategory> getCategories() {
        return categories;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        WatchlistUpdateRequest that = (WatchlistUpdateRequest) o;
        return Objects.equals(categories, that.categories);
    }

    @Override
    public int hashCode() {
        return Objects.hash(categories);
    }
}