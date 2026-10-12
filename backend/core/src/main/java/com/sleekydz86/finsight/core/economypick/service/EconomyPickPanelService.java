package com.sleekydz86.finsight.core.economypick.service;

import com.sleekydz86.finsight.core.economypick.domain.AssetRatioMath;
import com.sleekydz86.finsight.core.economypick.domain.EconomyPickAssets;
import com.sleekydz86.finsight.core.economypick.domain.EconomyPickSales;
import com.sleekydz86.finsight.core.economypick.domain.SalesMixMath;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioSummary;
import com.sleekydz86.finsight.core.portfolio.service.PortfolioService;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class EconomyPickPanelService {

    private final PortfolioService portfolioService;

    public EconomyPickPanelService(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    public EconomyPickAssets assets(Long userId) {
        if (userId == null) {
            return sample();
        }
        PortfolioSummary summary = portfolioService.dashboard(userId);
        EconomyPickAssets owned = AssetRatioMath.owned(summary.totalAssets(), summary.totalLiabilities());
        return owned == null ? sample() : owned;
    }

    public EconomyPickSales sales(String name, String symbol) {
        return SalesMixMath.predict(name, symbol);
    }

    private EconomyPickAssets sample() {
        return AssetRatioMath.sample(12 + ThreadLocalRandom.current().nextInt(36));
    }
}
