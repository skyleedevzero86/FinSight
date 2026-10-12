package com.sleekydz86.finsight.core.media.youtube.service;

import com.sleekydz86.finsight.core.board.domain.Board;
import com.sleekydz86.finsight.core.board.domain.BoardStatus;
import com.sleekydz86.finsight.core.board.domain.BoardType;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardPersistencePort;
import com.sleekydz86.finsight.core.global.exception.AiAnalysisFailedException;
import com.sleekydz86.finsight.core.media.youtube.adapter.requester.YoutubeAiContentRequester;
import com.sleekydz86.finsight.core.media.youtube.adapter.requester.YoutubeApiClient;
import com.sleekydz86.finsight.core.media.youtube.adapter.requester.properties.YoutubeApiProperties;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeGeneratedContent;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeImportStatus;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeVideoMeta;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeVideoDetailResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.out.YoutubeImportSourcePersistencePort;
import com.sleekydz86.finsight.core.media.youtube.domain.port.out.YoutubeVideoMetaPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YoutubeMediaEnrichVideoTest {

    @Mock
    private YoutubeVideoMetaPersistencePort videoMetaPort;

    @Mock
    private YoutubeImportSourcePersistencePort sourcePort;

    @Mock
    private BoardPersistencePort boardPort;

    @Mock
    private YoutubeApiClient youtubeApiClient;

    @Mock
    private YoutubeAiContentRequester aiContentRequester;

    private YoutubeMediaService service;
    private AtomicReference<YoutubeVideoMeta> stored;

    @BeforeEach
    void setUp() {
        stored = new AtomicReference<>(publishedMeta());
        when(videoMetaPort.findByBoardId(7L)).thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(videoMetaPort.save(any())).thenAnswer(invocation -> {
            YoutubeVideoMeta saved = invocation.getArgument(0);
            stored.set(saved);
            return saved;
        });
        when(boardPort.findById(7L)).thenReturn(Optional.of(board()));
        when(boardPort.findPreviousAndNext(7L, BoardType.MEDIA)).thenReturn(List.of());
        service = new YoutubeMediaService(
                videoMetaPort,
                sourcePort,
                boardPort,
                youtubeApiClient,
                aiContentRequester,
                new YoutubeApiProperties());
    }

    @Test
    void 게시_영상도_요약과_핵심포인트를_저장한다() {
        when(aiContentRequester.generate(any(), any())).thenReturn(YoutubeGeneratedContent.builder()
                .summary("국채 금리 흐름을 정리한 영상입니다.")
                .editorComment("제목에 나온 금리만 짚습니다.")
                .keyPoints(List.of("국채 금리", "순환매", "제목 범위"))
                .build());

        YoutubeVideoDetailResponse detail = service.enrichVideo(7L);

        assertThat(detail.getImportStatus()).isEqualTo(YoutubeImportStatus.PUBLISHED);
        assertThat(detail.getSummary()).isEqualTo("국채 금리 흐름을 정리한 영상입니다.");
        assertThat(detail.getEditorComment()).isEqualTo("제목에 나온 금리만 짚습니다.");
        assertThat(detail.getKeyPoints()).containsExactly("국채 금리", "순환매", "제목 범위");
        assertThat(detail.getAiStatus()).isEqualTo("DONE");
        assertThat(detail.getAiFailedAt()).isNull();
    }

    @Test
    void 생성에_실패하면_실패시각을_남긴다() {
        when(aiContentRequester.generate(any(), any()))
                .thenThrow(new AiAnalysisFailedException("llama3.2", "영상 보강 문장을 만들지 못했습니다."));

        YoutubeVideoDetailResponse detail = service.enrichVideo(7L);

        assertThat(detail.getAiStatus()).isEqualTo("FAILED");
        assertThat(detail.getAiFailedAt()).isNotNull();
        assertThat(detail.getSummary()).isNull();
    }

    private static YoutubeVideoMeta publishedMeta() {
        return YoutubeVideoMeta.builder()
                .id(7L)
                .boardId(7L)
                .videoId("45C717bFiE8")
                .youtubeTitle("10년물 국채 금리")
                .youtubeDescription("광고 구독 링크")
                .channelTitle("삼프로TV")
                .category("THEME")
                .importStatus(YoutubeImportStatus.PUBLISHED)
                .build();
    }

    private static Board board() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 7, 13, 0);
        return new Board(
                7L,
                "10년물 국채 금리",
                "광고 본문",
                "media-batch@finsight.local",
                BoardType.MEDIA,
                BoardStatus.ACTIVE,
                0,
                0,
                0,
                0,
                0,
                List.of(),
                List.of(),
                now,
                now,
                List.of(),
                false);
    }
}
