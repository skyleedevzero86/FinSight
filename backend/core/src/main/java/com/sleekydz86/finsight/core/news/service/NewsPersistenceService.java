package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;

import java.util.List;

@Service
public class NewsPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(NewsPersistenceService.class);

    private final NewsPersistencePort newsPersistencePort;

    public NewsPersistenceService(NewsPersistencePort newsPersistencePort) {
        this.newsPersistencePort = newsPersistencePort;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public Newses saveNewsToDatabase(List<News> newses) {
        try {
            log.info("데이터베이스에 {} 건의 뉴스 저장 시작", newses.size());
            Newses savedNewses = newsPersistencePort.saveAllNews(newses);
            log.info("데이터베이스 저장 완료: {} 건", savedNewses.getNewses().size());
            return savedNewses;
        } catch (Exception e) {
            log.error("데이터베이스 저장 실패", e);
            throw new RuntimeException("뉴스 저장 실패", e);
        }
    }
}
