package com.sleekydz86.finsight.core.board.service;

import com.sleekydz86.finsight.core.board.domain.port.out.BoardPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class BoardViewCountService {

    private static final Logger log = LoggerFactory.getLogger(BoardViewCountService.class);

    private final BoardPersistencePort boardPersistencePort;
    private final TransactionTemplate requiresNewTemplate;

    public BoardViewCountService(
            BoardPersistencePort boardPersistencePort,
            PlatformTransactionManager transactionManager) {
        this.boardPersistencePort = boardPersistencePort;
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.setReadOnly(false);
        this.requiresNewTemplate = template;
    }

    public void incrementSafely(Long boardId) {
        if (boardId == null || boardId <= 0) {
            return;
        }
        try {
            requiresNewTemplate.executeWithoutResult(status -> boardPersistencePort.incrementViewCount(boardId));
        } catch (Exception e) {
            log.warn("게시글 조회수 증가에 실패했습니다. boardId={}, cause={}", boardId, e.getMessage());
        }
    }
}
