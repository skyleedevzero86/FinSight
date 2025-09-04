package com.sleekydz86.finsight.core.user.service;

import com.sleekydz86.finsight.core.user.adapter.persistence.command.UserJpaRepository;
import com.sleekydz86.finsight.core.user.adapter.persistence.command.UserJpaEntity;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.global.exception.UserNotFoundException;
import com.sleekydz86.finsight.core.global.exception.InsufficientPermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class WatchlistService {

    private static final Logger log = LoggerFactory.getLogger(WatchlistService.class);

    private final UserJpaRepository userJpaRepository;

    public WatchlistService(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    public List<TargetCategory> getUserWatchlist(Long userId) {
        log.debug("사용자 관심 종목 조회: {}", userId);

        UserJpaEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return user.getWatchlist();
    }

    public void updateUserWatchlist(Long userId, List<TargetCategory> watchlist, String currentUserEmail) {
        log.debug("사용자 관심 종목 업데이트: {}", userId);

        UserJpaEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!user.getEmail().equals(currentUserEmail)) {
            throw new InsufficientPermissionException("관심 종목 수정 권한이 없습니다");
        }

        user.setWatchlist(watchlist);
        userJpaRepository.save(user);

        log.info("사용자 관심 종목 업데이트 완료: {} - {} 개 항목", userId, watchlist.size());
    }

    public void addToWatchlist(Long userId, TargetCategory category, String currentUserEmail) {
        log.debug("관심 종목 추가: {} - {}", userId, category);

        UserJpaEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!user.getEmail().equals(currentUserEmail)) {
            throw new InsufficientPermissionException("관심 종목 수정 권한이 없습니다");
        }

        List<TargetCategory> watchlist = user.getWatchlist();
        if (!watchlist.contains(category)) {
            watchlist.add(category);
            user.setWatchlist(watchlist);
            userJpaRepository.save(user);
            log.info("관심 종목 추가 완료: {} - {}", userId, category);
        }
    }

    public void removeFromWatchlist(Long userId, TargetCategory category, String currentUserEmail) {
        log.debug("관심 종목 제거: {} - {}", userId, category);

        UserJpaEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!user.getEmail().equals(currentUserEmail)) {
            throw new InsufficientPermissionException("관심 종목 수정 권한이 없습니다");
        }

        List<TargetCategory> watchlist = user.getWatchlist();
        if (watchlist.remove(category)) {
            user.setWatchlist(watchlist);
            userJpaRepository.save(user);
            log.info("관심 종목 제거 완료: {} - {}", userId, category);
        }
    }
}