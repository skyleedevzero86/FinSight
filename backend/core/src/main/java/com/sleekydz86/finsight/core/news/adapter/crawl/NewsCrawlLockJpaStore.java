package com.sleekydz86.finsight.core.news.adapter.crawl;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.crawl.NewsCrawlLockStore;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class NewsCrawlLockJpaStore implements NewsCrawlLockStore {

    private final NewsCrawlLockJpaRepository locks;

    public NewsCrawlLockJpaStore(NewsCrawlLockJpaRepository locks) {
        this.locks = locks;
    }

    @Override
    @Transactional
    public Optional<Long> acquire(NewsProvider provider, String owner, LocalDateTime expiresAt, LocalDateTime now) {
        NewsCrawlLockJpaEntity entity = locks.lockByProvider(provider.name()).orElse(null);
        if (entity == null) {
            entity = insert(provider, owner, expiresAt);
        }
        if (entity.getExpiresAt().isAfter(now) && !entity.getOwner().equals(owner)) {
            return Optional.empty();
        }
        long token = entity.getExpiresAt().isAfter(now) ? entity.getFencingToken() : entity.getFencingToken() + 1;
        entity.setOwner(owner);
        entity.setFencingToken(token);
        entity.setExpiresAt(expiresAt);
        return Optional.of(token);
    }

    @Override
    @Transactional
    public boolean release(NewsProvider provider, String owner, long fencingToken) {
        NewsCrawlLockJpaEntity entity = locks.lockByProvider(provider.name()).orElse(null);
        if (entity == null || !entity.getOwner().equals(owner) || entity.getFencingToken() != fencingToken) {
            return false;
        }
        entity.setExpiresAt(entity.getExpiresAt().minusYears(1));
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean held(NewsProvider provider, String owner, long fencingToken, LocalDateTime now) {
        NewsCrawlLockJpaEntity entity = locks.findById(provider.name()).orElse(null);
        if (entity == null || !entity.getOwner().equals(owner) || entity.getFencingToken() != fencingToken) {
            return false;
        }
        return entity.getExpiresAt().isAfter(now);
    }

    private NewsCrawlLockJpaEntity insert(NewsProvider provider, String owner, LocalDateTime expiresAt) {
        NewsCrawlLockJpaEntity created = new NewsCrawlLockJpaEntity();
        created.setProvider(provider.name());
        created.setOwner(owner);
        created.setFencingToken(0L);
        created.setExpiresAt(expiresAt.minusYears(1));
        try {
            return locks.saveAndFlush(created);
        } catch (DataIntegrityViolationException error) {
            return locks.lockByProvider(provider.name()).orElseThrow(() -> error);
        }
    }
}
