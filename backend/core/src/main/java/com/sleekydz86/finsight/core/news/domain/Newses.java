package com.sleekydz86.finsight.core.news.domain;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Newses {
    private final List<News> newses;

    public Newses() {
        this.newses = new ArrayList<>();
    }

    public Newses(List<News> newses) {
        this.newses = newses != null ? newses : new ArrayList<>();
    }

    public List<News> findAllNewsesFilteredOfCategoriesAndProviders(List<TargetCategory> categories,
            List<NewsProvider> providers) {
        List<NewsProvider> filteredProviders = getFilteredProviders(providers);

        return newses.stream()
                .filter(news -> news.isContainsNewsProviderAndCategories(categories, filteredProviders))
                .collect(Collectors.toList());
    }

    private List<NewsProvider> getFilteredProviders(List<NewsProvider> providers) {
        List<NewsProvider> filteredProviders = new ArrayList<>(providers);

        if (providers.isEmpty() || providers.contains(NewsProvider.ALL)) {
            filteredProviders = new ArrayList<>(NewsProvider.getAllProviders());
        }

        return filteredProviders;
    }

    public List<News> getNewses() {
        return newses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Newses newses1 = (Newses) o;
        return Objects.equals(newses, newses1.newses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(newses);
    }

    @Override
    public String toString() {
        return "Newses{" +
                "newses=" + newses +
                '}';
    }
}