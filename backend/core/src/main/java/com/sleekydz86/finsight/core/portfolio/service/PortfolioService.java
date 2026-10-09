package com.sleekydz86.finsight.core.portfolio.service;

import com.sleekydz86.finsight.core.global.exception.ValidationException;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioAssetJpaEntity;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioAssetJpaRepository;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareJpaEntity;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareJpaRepository;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioSnapshotJpaEntity;
import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioSnapshotJpaRepository;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioAssetCategory;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioAssetKind;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioHolding;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioMonthPoint;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioShareFeed;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioShareFeed.PortfolioShareCard;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioShareFeed.PortfolioShareGoal;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioSummary;
import com.sleekydz86.finsight.core.portfolio.domain.port.in.dto.PortfolioAssetCommand;
import com.sleekydz86.finsight.core.portfolio.domain.port.in.dto.PortfolioShareCommand;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
public class PortfolioService {

    private static final long MAX_AMOUNT = 100_000_000_000_000L;
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private static final DateTimeFormatter SHARE_DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final PortfolioAssetJpaRepository assetRepository;
    private final PortfolioSnapshotJpaRepository snapshotRepository;
    private final PortfolioShareJpaRepository shareRepository;
    private final PortfolioGoalSearch goalSearch;
    private final UserPersistencePort userPersistencePort;

    public PortfolioService(
            PortfolioAssetJpaRepository assetRepository,
            PortfolioSnapshotJpaRepository snapshotRepository,
            PortfolioShareJpaRepository shareRepository,
            PortfolioGoalSearch goalSearch,
            UserPersistencePort userPersistencePort) {
        this.assetRepository = assetRepository;
        this.snapshotRepository = snapshotRepository;
        this.shareRepository = shareRepository;
        this.goalSearch = goalSearch;
        this.userPersistencePort = userPersistencePort;
    }

    @Transactional(readOnly = true)
    public PortfolioSummary dashboard(Long userId) {
        requireUser(userId);
        return summarize(userId);
    }

    @Transactional
    public PortfolioSummary register(Long userId, PortfolioAssetCommand command) {
        requireUser(userId);
        CheckedAsset checked = check(command);
        assetRepository.save(new PortfolioAssetJpaEntity(
                userId,
                checked.name(),
                checked.kind(),
                checked.category(),
                checked.amount(),
                checked.profitRate(),
                checked.custodian(),
                checked.acquisitionAmount(),
                checked.quantity(),
                checked.unitPrice(),
                checked.memo()));
        return summarize(userId);
    }

    @Transactional
    public PortfolioSummary delete(Long userId, Long assetId) {
        requireUser(userId);
        PortfolioAssetJpaEntity asset = assetRepository.findByIdAndUserId(assetId, userId)
                .orElseThrow(() -> new ValidationException(
                        "자산을 찾을 수 없습니다. id=" + assetId,
                        List.of()));
        assetRepository.delete(asset);
        return summarize(userId);
    }

    @Transactional
    public PortfolioSummary recordCurrentMonth(Long userId, String memo) {
        requireUser(userId);
        List<PortfolioAssetJpaEntity> assets = assetsOf(userId);
        if (assets.isEmpty()) {
            throw new ValidationException("등록된 자산이 없어 이번 달 기록을 저장할 수 없습니다.", List.of());
        }
        String note = normalizeMemo(memo);
        Totals totals = totals(assets);
        String yearMonth = YearMonth.now(ZONE).toString();
        PortfolioSnapshotJpaEntity snapshot = snapshotRepository.findByUserIdAndYearMonth(userId, yearMonth)
                .orElse(null);
        LocalDateTime now = LocalDateTime.now();
        if (snapshot == null) {
            snapshotRepository.save(new PortfolioSnapshotJpaEntity(
                    userId, yearMonth, totals.assets(), totals.liabilities(), totals.netWorth(), now, note));
        } else {
            snapshot.replace(totals.assets(), totals.liabilities(), totals.netWorth(), now, note);
        }
        return summarize(userId);
    }

    @Transactional(readOnly = true)
    public PortfolioShareFeed listPublicShares(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 12);
        Page<PortfolioShareJpaEntity> found = shareRepository.findByVisibilityOrderByCreatedAtDescIdDesc(
                "PUBLIC", PageRequest.of(safePage, safeSize));
        List<PortfolioShareCard> cards = found.stream().map(this::toCard).toList();
        List<PortfolioShareGoal> goals = goalSearch.rank(weekTopics());
        return new PortfolioShareFeed(cards, goals, found.hasNext());
    }

    @Transactional
    public PortfolioSummary publish(Long userId, PortfolioShareCommand command) {
        requireUser(userId);
        if (command == null || command.message() == null || command.message().isBlank()) {
            throw new ValidationException("공유 설명을 입력해 주세요.", List.of());
        }
        String message = command.message().trim();
        if (message.length() > 500) {
            throw new ValidationException("공유 설명은 500자 이하로 입력해 주세요.", List.of());
        }
        PortfolioSummary summary = recordCurrentMonth(userId, message);
        String visibility = choice(command.visibility(), "PUBLIC", "FOLLOWERS", "PRIVATE");
        String amountMode = choice(command.amountMode(), "BAND", "EXACT", "RATIO");
        boolean showAmount = command.showNetWorth() && !"RATIO".equals(amountMode);
        shareRepository.save(new PortfolioShareJpaEntity(
                userId,
                authorName(userId),
                message,
                visibility,
                "1억 만들기",
                showAmount ? PortfolioShareText.amountLabel(amountMode, summary.netWorth()) : "",
                PortfolioShareText.progress(summary.goalPercentTenths()),
                command.showMonthRate() ? PortfolioShareText.signedPercent(summary.monthDelta(), summary.netWorth()) : null,
                command.showGoal() ? goalRate(summary.goalPercentTenths()) : null,
                command.showAllocation(),
                command.showDebt(),
                LocalDateTime.now()));
        return summary;
    }

    private PortfolioShareCard toCard(PortfolioShareJpaEntity share) {
        String sharedAt = share.getCreatedAt() == null ? "" : SHARE_DATE.format(share.getCreatedAt());
        return new PortfolioShareCard(
                share.getId(),
                PortfolioShareText.maskName(share.getAuthorName()),
                share.getMessage(),
                share.getGoalLabel(),
                share.getAmountLabel(),
                share.getProgressPercent(),
                share.getMonthRateLabel(),
                share.getGoalRateLabel(),
                share.isShowAsset(),
                share.isShowDebt(),
                share.getCheerCount(),
                sharedAt);
    }

    private List<PortfolioGoalSearch.PortfolioTopic> weekTopics() {
        return shareRepository.findByVisibilityAndCreatedAtGreaterThanEqual("PUBLIC", weekStart()).stream()
                .map(share -> new PortfolioGoalSearch.PortfolioTopic(
                        share.getId() == null ? 0L : share.getId(),
                        share.getUserId(),
                        share.getAuthorName(),
                        share.getGoalLabel(),
                        share.getMessage()))
                .toList();
    }

    private LocalDateTime weekStart() {
        LocalDate monday = LocalDate.now(ZONE).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return monday.atStartOfDay();
    }

    private String authorName(Long userId) {
        return userPersistencePort.findById(userId)
                .map(User::getUsername)
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .orElse("회원");
    }

    private String goalRate(int tenths) {
        int whole = tenths / 10;
        int fraction = Math.abs(tenths % 10);
        return fraction == 0 ? whole + "%" : whole + "." + fraction + "%";
    }

    private String choice(String raw, String fallback, String... allowed) {
        String value = raw == null || raw.isBlank() ? fallback : raw.trim().toUpperCase();
        if (fallback.equals(value)) {
            return value;
        }
        for (String item : allowed) {
            if (item.equals(value)) {
                return value;
            }
        }
        throw new ValidationException("공유 설정이 올바르지 않습니다. 입력값: " + raw, List.of());
    }

    private PortfolioSummary summarize(Long userId) {
        List<PortfolioAssetJpaEntity> assets = assetsOf(userId);
        List<PortfolioSnapshotJpaEntity> snapshots = snapshotRepository.findByUserIdOrderByYearMonthAsc(userId);
        YearMonth today = YearMonth.now(ZONE);
        boolean recorded = snapshots.stream().anyMatch(row -> today.toString().equals(row.getYearMonth()));
        return PortfolioSummaryCalculator.summarize(toHoldings(assets), toPoints(snapshots), today, recorded);
    }

    private List<PortfolioAssetJpaEntity> assetsOf(Long userId) {
        return assetRepository.findByUserIdOrderByValuationAmountDescIdAsc(userId);
    }

    private List<PortfolioHolding> toHoldings(List<PortfolioAssetJpaEntity> assets) {
        return assets.stream().map(this::toHolding).toList();
    }

    private PortfolioHolding toHolding(PortfolioAssetJpaEntity asset) {
        Double profit = asset.getProfitRate() == null ? null : asset.getProfitRate().doubleValue();
        return new PortfolioHolding(
                asset.getId(),
                asset.getAssetName(),
                asset.getAssetKind(),
                asset.getAssetCategory(),
                asset.getValuationAmount(),
                profit,
                asset.getAcquisitionAmount());
    }

    private List<PortfolioMonthPoint> toPoints(List<PortfolioSnapshotJpaEntity> snapshots) {
        return snapshots.stream().map(this::toPoint).toList();
    }

    private PortfolioMonthPoint toPoint(PortfolioSnapshotJpaEntity snapshot) {
        return new PortfolioMonthPoint(
                YearMonth.parse(snapshot.getYearMonth()),
                snapshot.getNetWorth(),
                snapshot.getTotalLiabilities());
    }

    private Totals totals(List<PortfolioAssetJpaEntity> assets) {
        long assetSum = 0;
        long liabilitySum = 0;
        for (PortfolioAssetJpaEntity asset : assets) {
            if (asset.getAssetKind() == PortfolioAssetKind.LIABILITY) {
                liabilitySum += asset.getValuationAmount();
            } else {
                assetSum += asset.getValuationAmount();
            }
        }
        return new Totals(assetSum, liabilitySum, assetSum - liabilitySum);
    }

    private CheckedAsset check(PortfolioAssetCommand command) {
        if (command == null) {
            throw new ValidationException("자산 정보가 없습니다.", List.of());
        }
        String name = normalizeName(command.name());
        PortfolioAssetKind kind = kindOf(command.kind());
        PortfolioAssetCategory category = categoryOf(kind, command.category());
        long amount = amountOf(command.amount());
        return new CheckedAsset(
                name,
                kind,
                category,
                amount,
                profitOf(command.profitRate()),
                clip(command.custodian(), 80, "계좌/보관처"),
                optionalAmount(command.acquisitionAmount(), "매수원금"),
                quantityOf(command.quantity()),
                optionalAmount(command.unitPrice(), "현재 단가"),
                clip(command.memo(), 500, "메모"));
    }

    private String normalizeName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty() || trimmed.length() > 80) {
            throw new ValidationException("자산 이름은 1자 이상 80자 이하여야 합니다. 입력값: " + name, List.of());
        }
        return trimmed;
    }

    private PortfolioAssetKind kindOf(String kind) {
        if (kind == null || kind.isBlank()) {
            throw new ValidationException("자산 구분은 ASSET 또는 LIABILITY 여야 합니다. 입력값: " + kind, List.of());
        }
        try {
            return PortfolioAssetKind.valueOf(kind.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("자산 구분은 ASSET 또는 LIABILITY 여야 합니다. 입력값: " + kind, List.of());
        }
    }

    private PortfolioAssetCategory categoryOf(PortfolioAssetKind kind, String category) {
        if (kind == PortfolioAssetKind.LIABILITY) {
            return PortfolioAssetCategory.LIABILITY;
        }
        if (category == null || category.isBlank()) {
            throw new ValidationException("자산 분류가 없습니다.", List.of());
        }
        try {
            PortfolioAssetCategory parsed = PortfolioAssetCategory.valueOf(category.trim().toUpperCase());
            if (parsed == PortfolioAssetCategory.LIABILITY) {
                throw new IllegalArgumentException(category);
            }
            return parsed;
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(
                    "자산 분류가 올바르지 않습니다. 입력값: " + category,
                    List.of());
        }
    }

    private long amountOf(long amount) {
        if (amount <= 0 || amount > MAX_AMOUNT) {
            throw new ValidationException("평가금액은 1원 이상 100조원 이하여야 합니다. 입력값: " + amount, List.of());
        }
        return amount;
    }

    private BigDecimal profitOf(Double profitRate) {
        if (profitRate == null) {
            return null;
        }
        if (profitRate < -100 || profitRate > 1000) {
            throw new ValidationException("손익률은 -100 이상 1000 이하여야 합니다. 입력값: " + profitRate, List.of());
        }
        return BigDecimal.valueOf(profitRate).setScale(2, RoundingMode.HALF_UP);
    }

    private String clip(String value, int max, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new ValidationException(label + "은 " + max + "자 이하여야 합니다. 입력값: " + value, List.of());
        }
        return trimmed;
    }

    private String normalizeMemo(String memo) {
        return clip(memo, 500, "이번 달 메모");
    }

    private Long optionalAmount(Long amount, String label) {
        if (amount == null) {
            return null;
        }
        if (amount < 0 || amount > MAX_AMOUNT) {
            throw new ValidationException(label + "은 0원 이상 100조원 이하여야 합니다. 입력값: " + amount, List.of());
        }
        return amount;
    }

    private BigDecimal quantityOf(Double quantity) {
        if (quantity == null) {
            return null;
        }
        if (quantity <= 0 || quantity > 1_000_000_000d) {
            throw new ValidationException("보유 수량은 0보다 커야 합니다. 입력값: " + quantity, List.of());
        }
        return BigDecimal.valueOf(quantity).setScale(4, RoundingMode.HALF_UP);
    }

    private void requireUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ValidationException("사용자 정보가 없습니다. id=" + userId, List.of());
        }
    }

    private record CheckedAsset(
            String name,
            PortfolioAssetKind kind,
            PortfolioAssetCategory category,
            long amount,
            BigDecimal profitRate,
            String custodian,
            Long acquisitionAmount,
            BigDecimal quantity,
            Long unitPrice,
            String memo
    ) {
    }

    private record Totals(long assets, long liabilities, long netWorth) {
    }
}
