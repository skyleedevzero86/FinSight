package com.sleekydz86.finsight.core.portfolio.domain;

public enum PortfolioAssetCategory {
    CASH("현금"),
    DEPOSIT("은행·예금"),
    STOCK_ETF("주식·ETF"),
    PENSION("연금"),
    REAL_ESTATE("부동산"),
    CAR("자동차"),
    CRYPTO("암호화폐"),
    OTHER("기타"),
    LIABILITY("부채");

    private final String label;

    PortfolioAssetCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
