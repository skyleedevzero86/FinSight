package com.sleekydz86.finsight.core.media.youtube.adapter.persistence.command;

import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeImportStatus;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeImportSourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface YoutubeVideoMetaJpaRepository extends JpaRepository<YoutubeVideoMetaJpaEntity, Long> {
    Optional<YoutubeVideoMetaJpaEntity> findByVideoId(String videoId);

    Optional<YoutubeVideoMetaJpaEntity> findByBoardId(Long boardId);

    List<YoutubeVideoMetaJpaEntity> findByBoardIdIn(List<Long> boardIds);

    Page<YoutubeVideoMetaJpaEntity> findByImportStatus(YoutubeImportStatus importStatus, Pageable pageable);

    Page<YoutubeVideoMetaJpaEntity> findByCategoryIgnoreCase(String category, Pageable pageable);

    Page<YoutubeVideoMetaJpaEntity> findByImportStatusAndCategoryIgnoreCase(
            YoutubeImportStatus importStatus,
            String category,
            Pageable pageable);

    Page<YoutubeVideoMetaJpaEntity> findByImportStatusAndAiGeneratedAtIsNull(
            YoutubeImportStatus importStatus,
            Pageable pageable);

    Page<YoutubeVideoMetaJpaEntity> findBySourceTypeAndSourceValue(
            YoutubeImportSourceType sourceType,
            String sourceValue,
            Pageable pageable);

    Page<YoutubeVideoMetaJpaEntity> findBySourceTypeAndSourceValueAndImportStatus(
            YoutubeImportSourceType sourceType,
            String sourceValue,
            YoutubeImportStatus importStatus,
            Pageable pageable);

    long countBySourceTypeAndSourceValue(
            YoutubeImportSourceType sourceType,
            String sourceValue);

    long countBySourceTypeAndSourceValueAndImportStatus(
            YoutubeImportSourceType sourceType,
            String sourceValue,
            YoutubeImportStatus importStatus);

    long countBySourceTypeAndSourceValueAndImportStatusAndAiGeneratedAtIsNull(
            YoutubeImportSourceType sourceType,
            String sourceValue,
            YoutubeImportStatus importStatus);

    @Query(
            value = """
                    SELECT e FROM YoutubeVideoMetaJpaEntity e
                    WHERE (:applyStatus = false OR e.importStatus = :importStatus)
                      AND (:category = '' OR LOWER(e.category) = LOWER(:category))
                      AND (:sourceValue = '' OR e.sourceValue = :sourceValue)
                      AND (
                        :keyword = ''
                        OR LOWER(e.youtubeTitle) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(COALESCE(e.channelTitle, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(COALESCE(e.summary, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                      )
                    """,
            countQuery = """
                    SELECT COUNT(e) FROM YoutubeVideoMetaJpaEntity e
                    WHERE (:applyStatus = false OR e.importStatus = :importStatus)
                      AND (:category = '' OR LOWER(e.category) = LOWER(:category))
                      AND (:sourceValue = '' OR e.sourceValue = :sourceValue)
                      AND (
                        :keyword = ''
                        OR LOWER(e.youtubeTitle) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(COALESCE(e.channelTitle, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(COALESCE(e.summary, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                      )
                    """)
    Page<YoutubeVideoMetaJpaEntity> searchAdmin(
            @Param("applyStatus") boolean applyStatus,
            @Param("importStatus") YoutubeImportStatus importStatus,
            @Param("category") String category,
            @Param("keyword") String keyword,
            @Param("sourceValue") String sourceValue,
            Pageable pageable);
}
