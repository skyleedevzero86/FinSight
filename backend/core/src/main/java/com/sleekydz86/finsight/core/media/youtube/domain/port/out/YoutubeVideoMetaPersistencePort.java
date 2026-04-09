package com.sleekydz86.finsight.core.media.youtube.domain.port.out;

import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeImportStatus;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeImportSourceType;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeVideoMeta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface YoutubeVideoMetaPersistencePort {
    YoutubeVideoMeta save(YoutubeVideoMeta videoMeta);

    Optional<YoutubeVideoMeta> findByVideoId(String videoId);

    Optional<YoutubeVideoMeta> findByBoardId(Long boardId);

    List<YoutubeVideoMeta> findByBoardIds(List<Long> boardIds);

    Page<YoutubeVideoMeta> search(YoutubeImportStatus importStatus, String category, Pageable pageable);

    Page<YoutubeVideoMeta> searchBySource(
            YoutubeImportSourceType sourceType,
            String sourceValue,
            YoutubeImportStatus importStatus,
            Pageable pageable);

    List<YoutubeVideoMeta> findPendingAiEnrichment(int limit);

    long countBySource(YoutubeImportSourceType sourceType, String sourceValue);

    long countBySourceAndImportStatus(
            YoutubeImportSourceType sourceType,
            String sourceValue,
            YoutubeImportStatus importStatus);

    long countPendingAiBySource(YoutubeImportSourceType sourceType, String sourceValue);
}
