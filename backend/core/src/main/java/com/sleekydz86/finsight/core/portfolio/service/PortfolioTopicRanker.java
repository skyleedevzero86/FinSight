package com.sleekydz86.finsight.core.portfolio.service;

import com.sleekydz86.finsight.core.portfolio.domain.PortfolioShareFeed.PortfolioShareGoal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PortfolioTopicRanker {

    public static final double SIMILAR = 0.72;
    public static final int LIMIT = 4;

    private PortfolioTopicRanker() {
    }

    public static List<PortfolioShareGoal> rank(List<PortfolioTopicPoint> points, int limit) {
        return rankByNeighbors(points, PortfolioTopicRanker::localNear, limit);
    }

    public static List<PortfolioShareGoal> rankByNeighbors(
            List<PortfolioTopicPoint> points,
            TopicNeighbors neighbors,
            int limit) {
        if (points == null || points.isEmpty() || limit <= 0) {
            return List.of();
        }
        List<PortfolioTopicPoint> pending = ordered(points);
        List<Cluster> clusters = new ArrayList<>();
        while (!pending.isEmpty()) {
            clusters.add(takeCluster(pending, neighbors));
        }
        return clusters.stream()
                .sorted(Comparator.comparingInt(Cluster::people).reversed().thenComparing(Cluster::label))
                .limit(limit)
                .map(cluster -> new PortfolioShareGoal(cluster.label(), cluster.people()))
                .toList();
    }

    public static boolean sameLabel(String left, String right) {
        String foldedLeft = fold(left);
        return !foldedLeft.isEmpty() && foldedLeft.equals(fold(right));
    }

    private static Cluster takeCluster(List<PortfolioTopicPoint> pending, TopicNeighbors neighbors) {
        PortfolioTopicPoint seed = pending.get(0);
        List<PortfolioTopicPoint> near = new ArrayList<>(neighbors.near(seed, pending));
        if (near.stream().noneMatch(point -> point.shareId() == seed.shareId())) {
            near.add(seed);
        }
        Set<Long> ids = new HashSet<>();
        for (PortfolioTopicPoint point : near) {
            ids.add(point.shareId());
        }
        pending.removeIf(point -> ids.contains(point.shareId()));
        return Cluster.from(near);
    }

    private static List<PortfolioTopicPoint> ordered(List<PortfolioTopicPoint> points) {
        List<PortfolioTopicPoint> pending = new ArrayList<>(points);
        pending.sort(Comparator
                .comparingInt((PortfolioTopicPoint point) -> -labelCount(points, point.label()))
                .thenComparing(PortfolioTopicPoint::label, Comparator.nullsLast(String::compareTo)));
        return pending;
    }

    private static List<PortfolioTopicPoint> localNear(PortfolioTopicPoint seed, List<PortfolioTopicPoint> pending) {
        List<PortfolioTopicPoint> near = new ArrayList<>();
        for (PortfolioTopicPoint point : pending) {
            if (point.shareId() == seed.shareId()
                    || sameLabel(seed.label(), point.label())
                    || PortfolioTopicVector.cosine(seed.vector(), point.vector()) >= SIMILAR) {
                near.add(point);
            }
        }
        return near;
    }

    private static int labelCount(List<PortfolioTopicPoint> points, String label) {
        int count = 0;
        for (PortfolioTopicPoint point : points) {
            if (sameLabel(label, point.label())) {
                count++;
            }
        }
        return count;
    }

    private static String fold(String label) {
        if (label == null) {
            return "";
        }
        return label.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    @FunctionalInterface
    public interface TopicNeighbors {
        List<PortfolioTopicPoint> near(PortfolioTopicPoint seed, List<PortfolioTopicPoint> pending);
    }

    private record Cluster(String label, int people) {
        private static Cluster from(List<PortfolioTopicPoint> members) {
            Map<String, Integer> labels = new HashMap<>();
            Set<String> people = new HashSet<>();
            for (PortfolioTopicPoint member : members) {
                String label = member.label() == null ? "" : member.label();
                labels.merge(label, 1, Integer::sum);
                people.add(member.personKey());
            }
            String label = labels.entrySet().stream()
                    .max(Comparator.comparingInt(Map.Entry<String, Integer>::getValue)
                            .thenComparing(entry -> -entry.getKey().length()))
                    .map(Map.Entry::getKey)
                    .orElse("");
            return new Cluster(label, people.size());
        }
    }
}
