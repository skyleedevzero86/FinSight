package com.sleekydz86.finsight.core.portfolio.service;

import com.sleekydz86.finsight.core.portfolio.domain.PortfolioShareFeed.PortfolioShareGoal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioTopicRankerTest {

    @Test
    @DisplayName("같은 목표는 한 순위로 모이고 사람 수로 정렬한다")
    void groupsTheSameGoalIntoOneRankAndSortsByPeople() {
        // given
        List<PortfolioTopicPoint> points = List.of(
                point(1, "studio_kim", "1억 만들기", "디자인 스튜디오를 운영하면서 2천만원을 모으는 게 제 목표입니다."),
                point(2, "month_park", "1억 만들기", "디자이너 생활을 하면서 부채를 줄이는 게 제 목표입니다."),
                point(3, "cash_lee", "비상금 3천", "비상금 3천만원을 먼저 채우기로 했습니다."),
                point(4, "home_choi", "내집 마련", "내집 마련 통장을 따로 두었습니다."));
        List<PortfolioShareGoal> ranked = PortfolioTopicRanker.rank(points, 4);

        // when

        // then
        assertThat(ranked).hasSize(3);
        assertThat(ranked.get(0).label()).isEqualTo("1억 만들기");
        assertThat(ranked.get(0).participantCount()).isEqualTo(2);
        assertThat(ranked).extracting(PortfolioShareGoal::participantCount).containsExactly(2L, 1L, 1L);
    }

    @Test
    @DisplayName("같은 사용자는 한 명으로 센다")
    void countsTheSameUserOnce() {
        // given
        List<PortfolioTopicPoint> points = List.of(
                new PortfolioTopicPoint(1, "user:7", "1억 만들기", vector("1억 만들기", "첫번째")),
                new PortfolioTopicPoint(2, "user:7", "1억 만들기", vector("1억 만들기", "두번째")));
        List<PortfolioShareGoal> ranked = PortfolioTopicRanker.rank(points, 4);

        // when

        // then
        assertThat(ranked).hasSize(1);
        assertThat(ranked.get(0).participantCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("엘라스틱 코사인 점수는 유사도로 되돌린다")
    void convertsElasticCosineScoreBackToSimilarity() {
        // given

        // when

        // then
        assertThat(PortfolioTopicVector.fromElasticsearchScore(1.0)).isEqualTo(1.0);
        assertThat(PortfolioTopicVector.fromElasticsearchScore(0.86)).isEqualTo(0.86 * 2 - 1);
    }

    private static PortfolioTopicPoint point(long id, String name, String label, String message) {
        return new PortfolioTopicPoint(id, PortfolioTopicPoint.personKey(0, name), label, vector(label, message));
    }

    private static float[] vector(String label, String message) {
        return PortfolioTopicVector.embed(label, message, 768);
    }
}
