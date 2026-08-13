package com.sleekydz86.finsight.core.media.youtube.adapter.persistence.command;

import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeImportStatus;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeImportSourceType;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeVideoMeta;
import com.sleekydz86.finsight.core.media.youtube.domain.port.out.YoutubeVideoMetaPersistencePort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class YoutubeVideoMetaRepositoryImpl implements YoutubeVideoMetaPersistencePort {

    private final YoutubeVideoMetaJpaRepository youtubeVideoMetaJpaRepository;

    public YoutubeVideoMetaRepositoryImpl(YoutubeVideoMetaJpaRepository youtubeVideoMetaJpaRepository) {
        this.youtubeVideoMetaJpaRepository = youtubeVideoMetaJpaRepository;
    }

    @Override
    public YoutubeVideoMeta save(YoutubeVideoMeta videoMeta) {
        YoutubeVideoMetaJpaEntity entity = toEntity(videoMeta);
        YoutubeVideoMetaJpaEntity savedEntity = youtubeVideoMetaJpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<YoutubeVideoMeta> findByVideoId(String videoId) {
        return youtubeVideoMetaJpaRepository.findByVideoId(videoId).map(this::toDomain);
    }

    @Override
    public Optional<YoutubeVideoMeta> findByBoardId(Long boardId) {
        return youtubeVideoMetaJpaRepository.findByBoardId(boardId).map(this::toDomain);
    }

    @Override
    public List<YoutubeVideoMeta> findByBoardIds(List<Long> boardIds) {
        if (boardIds == null || boardIds.isEmpty()) {
            return List.of();
        }

        return youtubeVideoMetaJpaRepository.findByBoardIdIn(boardIds).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Page<YoutubeVideoMeta> search(YoutubeImportStatus importStatus, String category, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                newestFirstSort());

        Page<YoutubeVideoMetaJpaEntity> page;
        if (importStatus != null && category != null && !category.isBlank()) {
            page = youtubeVideoMetaJpaRepository.findByImportStatusAndCategoryIgnoreCase(importStatus, category, sortedPageable);
        } else if (importStatus != null) {
            page = youtubeVideoMetaJpaRepository.findByImportStatus(importStatus, sortedPageable);
        } else if (category != null && !category.isBlank()) {
            page = youtubeVideoMetaJpaRepository.findByCategoryIgnoreCase(category, sortedPageable);
        } else {
            page = youtubeVideoMetaJpaRepository.findAll(sortedPageable);
        }

        List<YoutubeVideoMeta> content = page.getContent().stream()
                .map(this::toDomain)
                .toList();

        return new PageImpl<>(content, sortedPageable, page.getTotalElements());
    }

    @Override
    public Page<YoutubeVideoMeta> searchBySource(
            YoutubeImportSourceType sourceType,
            String sourceValue,
            YoutubeImportStatus importStatus,
            Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                newestFirstSort());

        Page<YoutubeVideoMetaJpaEntity> page;
        if (importStatus != null) {
            page = youtubeVideoMetaJpaRepository.findBySourceTypeAndSourceValueAndImportStatus(
                    sourceType,
                    sourceValue,
                    importStatus,
                    sortedPageable);
        } else {
            page = youtubeVideoMetaJpaRepository.findBySourceTypeAndSourceValue(
                    sourceType,
                    sourceValue,
                    sortedPageable);
        }

        List<YoutubeVideoMeta> content = page.getContent().stream()
                .map(this::toDomain)
                .toList();

        return new PageImpl<>(content, sortedPageable, page.getTotalElements());
    }

    @Override
    public List<YoutubeVideoMeta> findPendingAiEnrichment(int limit) {
        int resolvedLimit = limit <= 0 ? 10 : limit;
        Pageable pageable = PageRequest.of(0, resolvedLimit, oldestFirstSort());

        return youtubeVideoMetaJpaRepository.findByImportStatusAndAiGeneratedAtIsNull(
                        YoutubeImportStatus.DRAFT,
                        pageable)
                .getContent().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countBySource(YoutubeImportSourceType sourceType, String sourceValue) {
        return youtubeVideoMetaJpaRepository.countBySourceTypeAndSourceValue(sourceType, sourceValue);
    }

    @Override
    public long countBySourceAndImportStatus(
            YoutubeImportSourceType sourceType,
            String sourceValue,
            YoutubeImportStatus importStatus) {
        return youtubeVideoMetaJpaRepository.countBySourceTypeAndSourceValueAndImportStatus(
                sourceType,
                sourceValue,
                importStatus);
    }

    @Override
    public long countPendingAiBySource(YoutubeImportSourceType sourceType, String sourceValue) {
        return youtubeVideoMetaJpaRepository.countBySourceTypeAndSourceValueAndImportStatusAndAiGeneratedAtIsNull(
                sourceType,
                sourceValue,
                YoutubeImportStatus.DRAFT);
    }

    private static Sort newestFirstSort() {
        return Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("createdAt"));
    }

    private static Sort oldestFirstSort() {
        return Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));
    }

    private YoutubeVideoMeta toDomain(YoutubeVideoMetaJpaEntity entity) {
        return YoutubeVideoMeta.builder()
                .id(entity.getId())
                .boardId(entity.getBoardId())
                .videoId(entity.getVideoId())
                .channelId(entity.getChannelId())
                .channelTitle(entity.getChannelTitle())
                .sourceType(entity.getSourceType())
                .sourceValue(entity.getSourceValue())
                .category(entity.getCategory())
                .youtubeTitle(entity.getYoutubeTitle())
                .youtubeDescription(entity.getYoutubeDescription())
                .thumbnailUrl(entity.getThumbnailUrl())
                .publishedAt(entity.getPublishedAt())
                .duration(entity.getDuration())
                .embedUrl(entity.getEmbedUrl())
                .summary(entity.getSummary())
                .editorComment(entity.getEditorComment())
                .keyPoints(entity.getKeyPoints())
                .aiGeneratedAt(entity.getAiGeneratedAt())
                .importStatus(entity.getImportStatus())
                .syncedAt(entity.getSyncedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private YoutubeVideoMetaJpaEntity toEntity(YoutubeVideoMeta videoMeta) {
        YoutubeVideoMetaJpaEntity entity = new YoutubeVideoMetaJpaEntity();
        entity.setId(videoMeta.getId());
        entity.setBoardId(videoMeta.getBoardId());
        entity.setVideoId(videoMeta.getVideoId());
        entity.setChannelId(videoMeta.getChannelId());
        entity.setChannelTitle(videoMeta.getChannelTitle());
        entity.setSourceType(videoMeta.getSourceType());
        entity.setSourceValue(videoMeta.getSourceValue());
        entity.setCategory(videoMeta.getCategory());
        entity.setYoutubeTitle(videoMeta.getYoutubeTitle());
        entity.setYoutubeDescription(videoMeta.getYoutubeDescription());
        entity.setThumbnailUrl(videoMeta.getThumbnailUrl());
        entity.setPublishedAt(videoMeta.getPublishedAt());
        entity.setDuration(videoMeta.getDuration());
        entity.setEmbedUrl(videoMeta.getEmbedUrl());
        entity.setSummary(videoMeta.getSummary());
        entity.setEditorComment(videoMeta.getEditorComment());
        entity.setKeyPoints(videoMeta.getKeyPoints());
        entity.setAiGeneratedAt(videoMeta.getAiGeneratedAt());
        entity.setImportStatus(videoMeta.getImportStatus());
        entity.setSyncedAt(videoMeta.getSyncedAt());
        entity.setCreatedAt(videoMeta.getCreatedAt());
        entity.setUpdatedAt(videoMeta.getUpdatedAt());
        return entity;
    }
}
