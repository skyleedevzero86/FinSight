package com.sleekydz86.finsight.core.portfolio.service;

import com.sleekydz86.finsight.core.portfolio.domain.PortfolioShareFeed.PortfolioShareGoal;
import com.sleekydz86.finsight.core.search.adapter.PortfolioTopicElasticSearch;
import com.sleekydz86.finsight.core.search.config.ElasticsearchProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

@Service
public class PortfolioGoalSearch {

    private static final Logger log = LoggerFactory.getLogger(PortfolioGoalSearch.class);

    private final ElasticsearchProperties properties;
    private final PortfolioTopicElasticSearch elasticSearch;

    public PortfolioGoalSearch(
            ElasticsearchProperties properties,
            @Autowired(required = false) PortfolioTopicElasticSearch elasticSearch) {
        this.properties = properties;
        this.elasticSearch = elasticSearch;
    }

    public List<PortfolioShareGoal> rank(List<PortfolioTopic> topics) {
        List<PortfolioTopicPoint> points = pointsOf(topics);
        if (properties.isEnabled() && elasticSearch != null) {
            try {
                return elasticSearch.rank(points);
            } catch (RestClientException | IllegalStateException | IllegalArgumentException exception) {
                log.warn("Elasticsearch 목표 벡터 집계에 실패해 벡터 검색으로 집계합니다. topics={}", points.size(), exception);
            }
        }
        return PortfolioTopicRanker.rank(points, PortfolioTopicRanker.LIMIT);
    }

    private List<PortfolioTopicPoint> pointsOf(List<PortfolioTopic> topics) {
        int dims = properties.getVectorDims();
        List<PortfolioTopicPoint> points = new ArrayList<>();
        if (topics == null) {
            return points;
        }
        for (PortfolioTopic topic : topics) {
            points.add(new PortfolioTopicPoint(
                    topic.shareId(),
                    topic.personKey(),
                    topic.label(),
                    PortfolioTopicVector.embed(topic.label(), topic.message(), dims)));
        }
        return points;
    }

    public record PortfolioTopic(long shareId, long userId, String authorName, String label, String message) {
        public String personKey() {
            return PortfolioTopicPoint.personKey(userId, authorName);
        }
    }
}
