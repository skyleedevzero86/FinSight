package com.sleekydz86.finsight.core.portfolio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.news.adapter.requester.overview.properties.OllamaProperties;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareJpaEntity;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareJpaRepository;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareReportJpaEntity;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareReportJpaRepository;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioShareDetection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioShareDetectionServiceTest {

    @Mock
    private PortfolioShareJpaRepository shareRepository;

    @Mock
    private PortfolioShareReportJpaRepository reportRepository;

    @Mock
    private WebClient ollamaWebClient;

    private PortfolioShareDetectionService detectionService;

    @BeforeEach
    void setUp() {
        detectionService = new PortfolioShareDetectionService(
                shareRepository,
                reportRepository,
                ollamaWebClient,
                new OllamaProperties(),
                new ObjectMapper());
    }

    @Test
    @DisplayName("문구와 신고 사유를 함께 센다")
    void countsTextTogetherWithReportReason() {
        // given
        when(shareRepository.findByVisibilityOrderByCreatedAtDescIdDesc(eq("PUBLIC"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(share())));
        when(reportRepository.findAll()).thenReturn(List.of(
                new PortfolioShareReportJpaEntity(9L, 3L, "투자 사기", "계좌번호를 요구합니다", LocalDateTime.of(2026, 10, 10, 1, 0))));

        // when
        PortfolioShareDetection detection = detectionService.detect();

        // then
        assertThat(detection.source()).isEqualTo("RULE");
        assertThat(detection.chips()).extracting(PortfolioShareDetection.Chip::label)
                .contains("오픈채팅 URL", "투자 사기");
        assertThat(detection.chips()).allSatisfy(chip -> assertThat(chip.count()).isEqualTo(1));
    }

    private static PortfolioShareJpaEntity share() {
        PortfolioShareJpaEntity entity = new PortfolioShareJpaEntity(
                0L,
                "month_park",
                "오픈채팅으로 연락 주세요. https://open.kakao.com/o/example",
                "PUBLIC",
                "1억 만들기",
                "2,000만원",
                60,
                null,
                null,
                false,
                false,
                LocalDateTime.of(2026, 10, 9, 8, 40));
        ReflectionTestUtils.setField(entity, "id", 4L);
        return entity;
    }
}
