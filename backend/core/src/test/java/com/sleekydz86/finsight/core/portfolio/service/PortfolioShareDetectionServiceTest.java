package com.sleekydz86.finsight.core.portfolio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.news.adapter.requester.overview.properties.OllamaProperties;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareJpaEntity;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareJpaRepository;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareReportJpaEntity;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareReportJpaRepository;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioShareDetection;
import org.junit.jupiter.api.BeforeEach;
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
    void 문구와_신고_사유를_함께_센다() {
        when(shareRepository.findByVisibilityOrderByCreatedAtDescIdDesc(eq("PUBLIC"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(share())));
        when(reportRepository.findAll()).thenReturn(List.of(
                new PortfolioShareReportJpaEntity(9L, 3L, "투자 사기", "계좌번호를 요구합니다", LocalDateTime.of(2026, 10, 10, 1, 0))));

        PortfolioShareDetection detection = detectionService.detect();

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
