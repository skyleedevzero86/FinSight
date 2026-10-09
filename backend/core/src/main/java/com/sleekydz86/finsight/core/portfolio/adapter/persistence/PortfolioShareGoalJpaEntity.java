package com.sleekydz86.finsight.core.portfolio.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "portfolio_share_goal")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioShareGoalJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "goal_label", nullable = false, length = 40)
    private String goalLabel;

    @Column(name = "participant_count", nullable = false)
    private int participantCount;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
