package com.sleekydz86.finsight.core.board.adapter.persistence.command;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.board.domain.BoardModerationRun;
import com.sleekydz86.finsight.core.board.domain.BoardModerationTarget;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardModerationRunPersistencePort;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class BoardModerationRunRepositoryImpl implements BoardModerationRunPersistencePort {

    private final BoardModerationRunJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public BoardModerationRunRepositoryImpl(
            BoardModerationRunJpaRepository jpaRepository,
            ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public BoardModerationRun save(BoardModerationRun run) {
        BoardModerationRunJpaEntity entity = new BoardModerationRunJpaEntity();
        entity.setTriggeredBy(run.triggeredBy());
        entity.setActorEmail(run.actorEmail());
        entity.setReportThreshold(run.reportThreshold());
        entity.setHiddenCount(run.hiddenCount());
        entity.setDetailsJson(writeTargets(run.targets()));
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<BoardModerationRun> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<BoardModerationRun> findRecent(int limit) {
        int safe = Math.max(1, Math.min(100, limit));
        return jpaRepository.findRecent(safe).stream().map(this::toDomain).toList();
    }

    private BoardModerationRun toDomain(BoardModerationRunJpaEntity entity) {
        return new BoardModerationRun(
                entity.getId(),
                entity.getTriggeredBy(),
                entity.getActorEmail(),
                entity.getReportThreshold(),
                entity.getHiddenCount(),
                readTargets(entity.getDetailsJson()),
                entity.getCreatedAt());
    }

    private String writeTargets(List<BoardModerationTarget> targets) {
        try {
            return objectMapper.writeValueAsString(targets == null ? List.of() : targets);
        } catch (Exception e) {
            throw new IllegalStateException("모더레이션 상세 JSON 직렬화 실패", e);
        }
    }

    private List<BoardModerationTarget> readTargets(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
