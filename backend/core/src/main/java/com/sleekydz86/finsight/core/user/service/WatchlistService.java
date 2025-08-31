package com.sleekydz86.finsight.core.user.service;

import com.sleekydz86.finsight.core.user.adapter.persistence.command.UserJpaRepository;
import com.sleekydz86.finsight.core.user.adapter.persistence.command.UserJpaEntity;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.global.exception.UserNotFoundException;
import com.sleekydz86.finsight.core.global.exception.InsufficientPermissionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WatchlistService {

    private final UserJpaRepository userJpaRepository;

    public List<TargetCategory> getUserWatchlist(Long userId) {
        log.debug("사용자 관심 종목 조회: {}", userId);

        UserJpaEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<TargetCategory> watchlist = user.getWatchlist();
        log.debug("사용자 {}의 관심 종목: {}", userId, watchlist);

        return watchlist;
    }

    public void addAssetToWatchlist(Long userId, TargetCategory asset) {
        log.info("사용자 관심 종목에 자산 추가: {} -> {}", userId, asset);

        UserJpaEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<TargetCategory> currentWatchlist = user.getWatchlist();

        if (!currentWatchlist.contains(asset)) {
            currentWatchlist.add(asset);
            user.setWatchlist(currentWatchlist);
            userJpaRepository.save(user);
            log.info("자산 {}이 사용자 {}의 관심 종목에 추가됨", asset, userId);
        } else {
            log.debug("자산 {}은 이미 사용자 {}의 관심 종목에 존재함", asset, userId);
        }
    }

    public void removeAssetFromWatchlist(Long userId, TargetCategory asset) {
        log.info("사용자 관심 종목에서 자산 제거: {} -> {}", userId, asset);

        UserJpaEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<TargetCategory> currentWatchlist = user.getWatchlist();

        if (currentWatchlist.remove(asset)) {
            user.setWatchlist(currentWatchlist);
            userJpaRepository.save(user);
            log.info("자산 {}이 사용자 {}의 관심 종목에서 제거됨", asset, userId);
        } else {
            log.debug("자산 {}은 사용자 {}의 관심 종목에 존재하지 않음", asset, userId);
        }
    }

    public void replaceWatchlist(Long userId, List<TargetCategory> newWatchlist) {
        log.info("사용자 관심 종목 전체 교체: {} -> {}", userId, newWatchlist);

        UserJpaEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.setWatchlist(newWatchlist);
        userJpaRepository.save(user);

        log.info("사용자 {}의 관심 종목이 {}로 교체됨", userId, newWatchlist);
    }

    public List<UserJpaEntity> findUsersByAsset(TargetCategory asset) {
        log.debug("자산 {}을 관심 종목으로 가진 사용자들 조회", asset);

        return userJpaRepository.findByWatchlistCategories(List.of(asset));
    }

    public List<UserJpaEntity> findUsersByAssets(List<TargetCategory> assets) {
        log.debug("자산들 {}을 관심 종목으로 가진 사용자들 조회", assets);

        return userJpaRepository.findByWatchlistCategories(assets);
    }

    public boolean isAssetInWatchlist(Long userId, TargetCategory asset) {
        log.debug("사용자 {}의 관심 종목에 자산 {} 포함 여부 확인", userId, asset);

        UserJpaEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return user.getWatchlist().contains(asset);
    }

    public int getWatchlistSize(Long userId) {
        log.debug("사용자 {}의 관심 종목 개수 조회", userId);

        UserJpaEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return user.getWatchlist().size();
    }

    public boolean isWatchlistEmpty(Long userId) {
        return getWatchlistSize(userId) == 0;
    }

    public void addAssetToWatchlistWithPermission(Long userId, TargetCategory asset, String requesterEmail) {
        log.info("권한 확인 후 사용자 관심 종목에 자산 추가: {} -> {} (요청자: {})", userId, asset, requesterEmail);

        UserJpaEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!user.getEmail().equals(requesterEmail) && !isAdmin(requesterEmail)) {
            log.warn("권한 부족: {}가 사용자 {}의 관심 종목을 수정하려 함", requesterEmail, userId);
            throw new InsufficientPermissionException("WATCHLIST_MODIFY");
        }

        addAssetToWatchlist(userId, asset);
    }

    public void removeAssetFromWatchlistWithPermission(Long userId, TargetCategory asset, String requesterEmail) {
        log.info("권한 확인 후 사용자 관심 종목에서 자산 제거: {} -> {} (요청자: {})", userId, asset, requesterEmail);

        UserJpaEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!user.getEmail().equals(requesterEmail) && !isAdmin(requesterEmail)) {
            log.warn("권한 부족: {}가 사용자 {}의 관심 종목을 수정하려 함", requesterEmail, userId);
            throw new InsufficientPermissionException("WATCHLIST_MODIFY");
        }

        removeAssetFromWatchlist(userId, asset);
    }

    private boolean isAdmin(String email) {
        return userJpaRepository.findByEmail(email)
                .map(user -> user.getRole().name().equals("ADMIN"))
                .orElse(false);
    }

}