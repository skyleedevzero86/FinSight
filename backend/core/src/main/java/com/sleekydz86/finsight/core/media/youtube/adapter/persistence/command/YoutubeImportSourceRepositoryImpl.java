package com.sleekydz86.finsight.core.media.youtube.adapter.persistence.command;

import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeImportSource;
import com.sleekydz86.finsight.core.media.youtube.domain.port.out.YoutubeImportSourcePersistencePort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class YoutubeImportSourceRepositoryImpl implements YoutubeImportSourcePersistencePort {

    private final YoutubeImportSourceJpaRepository youtubeImportSourceJpaRepository;

    public YoutubeImportSourceRepositoryImpl(YoutubeImportSourceJpaRepository youtubeImportSourceJpaRepository) {
        this.youtubeImportSourceJpaRepository = youtubeImportSourceJpaRepository;
    }

    @Override
    public YoutubeImportSource save(YoutubeImportSource source) {
        YoutubeImportSourceJpaEntity entity = toEntity(source);
        YoutubeImportSourceJpaEntity savedEntity = youtubeImportSourceJpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<YoutubeImportSource> findById(Long sourceId) {
        return youtubeImportSourceJpaRepository.findById(sourceId).map(this::toDomain);
    }

    @Override
    public List<YoutubeImportSource> findAll() {
        return youtubeImportSourceJpaRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<YoutubeImportSource> findActiveSources() {
        return youtubeImportSourceJpaRepository.findByActiveTrueOrderByCreatedAtDesc().stream()
                .map(this::toDomain)
                .toList();
    }

    private YoutubeImportSource toDomain(YoutubeImportSourceJpaEntity entity) {
        return YoutubeImportSource.builder()
                .id(entity.getId())
                .sourceType(entity.getSourceType())
                .sourceValue(entity.getSourceValue())
                .category(entity.getCategory())
                .active(entity.isActive())
                .autoPublish(entity.isAutoPublish())
                .lastSyncedAt(entity.getLastSyncedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private YoutubeImportSourceJpaEntity toEntity(YoutubeImportSource source) {
        YoutubeImportSourceJpaEntity entity = new YoutubeImportSourceJpaEntity();
        entity.setId(source.getId());
        entity.setSourceType(source.getSourceType());
        entity.setSourceValue(source.getSourceValue());
        entity.setCategory(source.getCategory());
        entity.setActive(source.isActive());
        entity.setAutoPublish(source.isAutoPublish());
        entity.setLastSyncedAt(source.getLastSyncedAt());
        entity.setCreatedAt(source.getCreatedAt());
        entity.setUpdatedAt(source.getUpdatedAt());
        return entity;
    }
}
