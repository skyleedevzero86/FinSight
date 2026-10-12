package com.sleekydz86.finsight.core.portfolio.service;

import com.sleekydz86.finsight.core.portfolio.domain.PortfolioDiagnosis;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioSummary;
import org.springframework.stereotype.Service;

@Service
public class PortfolioDiagnosisService {

    private final PortfolioService portfolioService;
    private final PortfolioLlamaNarrator narrator;

    public PortfolioDiagnosisService(PortfolioService portfolioService, PortfolioLlamaNarrator narrator) {
        this.portfolioService = portfolioService;
        this.narrator = narrator;
    }

    public PortfolioDiagnosis diagnose(Long userId) {
        PortfolioSummary summary = portfolioService.dashboard(userId);
        return narrator.narrate(summary);
    }
}
