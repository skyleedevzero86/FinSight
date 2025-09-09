package com.sleekydz86.finsight.core.global.validation;

import com.sleekydz86.finsight.core.global.exception.ValidationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ValidationService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PASSWORD_PATTERN = Pattern
            .compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{12,}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9가-힣_]{2,50}$");

    public void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("이메일은 필수입니다", List.of("email"));
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")) {
            throw new ValidationException("올바른 이메일 형식이 아닙니다", List.of("email"));
        }
    }

    public void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new ValidationException("비밀번호는 필수입니다", List.of("password"));
        }
        if (password.length() < 8) {
            throw new ValidationException("비밀번호는 최소 8자 이상이어야 합니다", List.of("password"));
        }
        if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]")) {
            throw new ValidationException("비밀번호는 대소문자, 숫자, 특수문자를 포함해야 합니다", List.of("password"));
        }
    }

    public void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("사용자명은 필수입니다", List.of("username"));
        }
        if (username.length() < 2 || username.length() > 50) {
            throw new ValidationException("사용자명은 2자 이상 50자 이하여야 합니다", List.of("username"));
        }
    }

    public void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new ValidationException("제목은 필수입니다", List.of("title"));
        }
        if (title.length() > 200) {
            throw new ValidationException("제목은 200자 이하여야 합니다", List.of("title"));
        }
    }

    public void validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new ValidationException("내용은 필수입니다", List.of("content"));
        }
        if (content.length() > 5000) {
            throw new ValidationException("내용은 5000자 이하여야 합니다", List.of("content"));
        }
    }

    public void validateUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new ValidationException("URL은 필수입니다", List.of("url"));
        }
        if (!url.matches("^https?://[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?$")) {
            throw new ValidationException("올바른 URL 형식이 아닙니다", List.of("url"));
        }
    }

    public void validatePageNumber(int page) {
        if (page < 0) {
            throw new ValidationException("페이지 번호는 0 이상이어야 합니다", List.of("page"));
        }
    }

    public void validatePageSize(int size) {
        if (size <= 0 || size > 100) {
            throw new ValidationException("페이지 크기는 1 이상 100 이하여야 합니다", List.of("size"));
        }
    }

    public void validateSearchKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new ValidationException("검색 키워드는 필수입니다", List.of("keyword"));
        }
        if (keyword.length() > 100) {
            throw new ValidationException("검색 키워드는 100자 이하여야 합니다", List.of("keyword"));
        }
    }

    public void validateCommentContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new ValidationException("댓글 내용은 필수입니다", List.of("content"));
        }
        if (content.length() > 1000) {
            throw new ValidationException("댓글 내용은 1000자 이하여야 합니다", List.of("content"));
        }
    }

    public void validateReportReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new ValidationException("신고 사유는 필수입니다", List.of("reason"));
        }
        if (reason.length() > 500) {
            throw new ValidationException("신고 사유는 500자 이하여야 합니다", List.of("reason"));
        }
    }

    public void validateNewsTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new ValidationException("뉴스 제목은 필수입니다", List.of("title"));
        }
        if (title.length() > 300) {
            throw new ValidationException("뉴스 제목은 300자 이하여야 합니다", List.of("title"));
        }
    }

    public void validateNewsContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new ValidationException("뉴스 내용은 필수입니다", List.of("content"));
        }
        if (content.length() > 10000) {
            throw new ValidationException("뉴스 내용은 10000자 이하여야 합니다", List.of("content"));
        }
    }

    public void validateNewsUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new ValidationException("뉴스 URL은 필수입니다", List.of("url"));
        }
        if (!url.matches("^https?://[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?$")) {
            throw new ValidationException("올바른 뉴스 URL 형식이 아닙니다", List.of("url"));
        }
    }

    public void validateNewsSource(String source) {
        if (source == null || source.trim().isEmpty()) {
            throw new ValidationException("뉴스 출처는 필수입니다", List.of("source"));
        }
        if (source.length() > 100) {
            throw new ValidationException("뉴스 출처는 100자 이하여야 합니다", List.of("source"));
        }
    }

    public void validateNewsCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            throw new ValidationException("뉴스 카테고리는 필수입니다", List.of("category"));
        }
        if (category.length() > 50) {
            throw new ValidationException("뉴스 카테고리는 50자 이하여야 합니다", List.of("category"));
        }
    }

    public void validateNewsPublishDate(LocalDateTime publishDate) {
        if (publishDate == null) {
            throw new ValidationException("뉴스 발행일은 필수입니다", List.of("publishDate"));
        }
        if (publishDate.isAfter(LocalDateTime.now())) {
            throw new ValidationException("뉴스 발행일은 현재 시간보다 이전이어야 합니다", List.of("publishDate"));
        }
    }

    public void validateNewsSentiment(String sentiment) {
        if (sentiment == null || sentiment.trim().isEmpty()) {
            throw new ValidationException("뉴스 감정은 필수입니다", List.of("sentiment"));
        }
        if (!sentiment.matches("^(POSITIVE|NEGATIVE|NEUTRAL)$")) {
            throw new ValidationException("뉴스 감정은 POSITIVE, NEGATIVE, NEUTRAL 중 하나여야 합니다", List.of("sentiment"));
        }
    }

    public void validateNewsSentimentScore(double score) {
        if (score < -1.0 || score > 1.0) {
            throw new ValidationException("뉴스 감정 점수는 -1.0과 1.0 사이여야 합니다", List.of("sentimentScore"));
        }
    }

    public void validateNewsConfidence(double confidence) {
        if (confidence < 0.0 || confidence > 1.0) {
            throw new ValidationException("뉴스 신뢰도는 0.0과 1.0 사이여야 합니다", List.of("confidence"));
        }
    }

    public void validateNewsRelevance(double relevance) {
        if (relevance < 0.0 || relevance > 1.0) {
            throw new ValidationException("뉴스 관련성은 0.0과 1.0 사이여야 합니다", List.of("relevance"));
        }
    }

    public void validateNewsImpact(String impact) {
        if (impact == null || impact.trim().isEmpty()) {
            throw new ValidationException("뉴스 영향도는 필수입니다", List.of("impact"));
        }
        if (!impact.matches("^(HIGH|MEDIUM|LOW)$")) {
            throw new ValidationException("뉴스 영향도는 HIGH, MEDIUM, LOW 중 하나여야 합니다", List.of("impact"));
        }
    }

    public void validateNewsPriority(int priority) {
        if (priority < 1 || priority > 5) {
            throw new ValidationException("뉴스 우선순위는 1과 5 사이여야 합니다", List.of("priority"));
        }
    }

    public void validateNewsTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            throw new ValidationException("뉴스 태그는 필수입니다", List.of("tags"));
        }
        if (tags.size() > 10) {
            throw new ValidationException("뉴스 태그는 최대 10개까지 가능합니다", List.of("tags"));
        }
        for (String tag : tags) {
            if (tag == null || tag.trim().isEmpty()) {
                throw new ValidationException("뉴스 태그는 빈 값일 수 없습니다", List.of("tags"));
            }
            if (tag.length() > 50) {
                throw new ValidationException("뉴스 태그는 50자 이하여야 합니다", List.of("tags"));
            }
        }
    }

    public void validateNewsKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            throw new ValidationException("뉴스 키워드는 필수입니다", List.of("keywords"));
        }
        if (keywords.size() > 20) {
            throw new ValidationException("뉴스 키워드는 최대 20개까지 가능합니다", List.of("keywords"));
        }
        for (String keyword : keywords) {
            if (keyword == null || keyword.trim().isEmpty()) {
                throw new ValidationException("뉴스 키워드는 빈 값일 수 없습니다", List.of("keywords"));
            }
            if (keyword.length() > 100) {
                throw new ValidationException("뉴스 키워드는 100자 이하여야 합니다", List.of("keywords"));
            }
        }
    }

    public void validateNewsEntities(List<String> entities) {
        if (entities == null || entities.isEmpty()) {
            throw new ValidationException("뉴스 엔티티는 필수입니다", List.of("entities"));
        }
        if (entities.size() > 50) {
            throw new ValidationException("뉴스 엔티티는 최대 50개까지 가능합니다", List.of("entities"));
        }
        for (String entity : entities) {
            if (entity == null || entity.trim().isEmpty()) {
                throw new ValidationException("뉴스 엔티티는 빈 값일 수 없습니다", List.of("entities"));
            }
            if (entity.length() > 200) {
                throw new ValidationException("뉴스 엔티티는 200자 이하여야 합니다", List.of("entities"));
            }
        }
    }

    public void validateNewsSummary(String summary) {
        if (summary == null || summary.trim().isEmpty()) {
            throw new ValidationException("뉴스 요약은 필수입니다", List.of("summary"));
        }
        if (summary.length() > 1000) {
            throw new ValidationException("뉴스 요약은 1000자 이하여야 합니다", List.of("summary"));
        }
    }

    public void validateNewsOverview(String overview) {
        if (overview == null || overview.trim().isEmpty()) {
            throw new ValidationException("뉴스 개요는 필수입니다", List.of("overview"));
        }
        if (overview.length() > 2000) {
            throw new ValidationException("뉴스 개요는 2000자 이하여야 합니다", List.of("overview"));
        }
    }

    public void validateNewsAnalysis(String analysis) {
        if (analysis == null || analysis.trim().isEmpty()) {
            throw new ValidationException("뉴스 분석은 필수입니다", List.of("analysis"));
        }
        if (analysis.length() > 5000) {
            throw new ValidationException("뉴스 분석은 5000자 이하여야 합니다", List.of("analysis"));
        }
    }

    public void validateNewsRecommendation(String recommendation) {
        if (recommendation == null || recommendation.trim().isEmpty()) {
            throw new ValidationException("뉴스 추천은 필수입니다", List.of("recommendation"));
        }
        if (recommendation.length() > 1000) {
            throw new ValidationException("뉴스 추천은 1000자 이하여야 합니다", List.of("recommendation"));
        }
    }

    public void validateNewsRisk(String risk) {
        if (risk == null || risk.trim().isEmpty()) {
            throw new ValidationException("뉴스 위험도는 필수입니다", List.of("risk"));
        }
        if (!risk.matches("^(HIGH|MEDIUM|LOW)$")) {
            throw new ValidationException("뉴스 위험도는 HIGH, MEDIUM, LOW 중 하나여야 합니다", List.of("risk"));
        }
    }

    public void validateNewsOpportunity(String opportunity) {
        if (opportunity == null || opportunity.trim().isEmpty()) {
            throw new ValidationException("뉴스 기회는 필수입니다", List.of("opportunity"));
        }
        if (!opportunity.matches("^(HIGH|MEDIUM|LOW)$")) {
            throw new ValidationException("뉴스 기회는 HIGH, MEDIUM, LOW 중 하나여야 합니다", List.of("opportunity"));
        }
    }

    public void validateNewsVolatility(String volatility) {
        if (volatility == null || volatility.trim().isEmpty()) {
            throw new ValidationException("뉴스 변동성은 필수입니다", List.of("volatility"));
        }
        if (!volatility.matches("^(HIGH|MEDIUM|LOW)$")) {
            throw new ValidationException("뉴스 변동성은 HIGH, MEDIUM, LOW 중 하나여야 합니다", List.of("volatility"));
        }
    }

    public void validateNewsLiquidity(String liquidity) {
        if (liquidity == null || liquidity.trim().isEmpty()) {
            throw new ValidationException("뉴스 유동성은 필수입니다", List.of("liquidity"));
        }
        if (!liquidity.matches("^(HIGH|MEDIUM|LOW)$")) {
            throw new ValidationException("뉴스 유동성은 HIGH, MEDIUM, LOW 중 하나여야 합니다", List.of("liquidity"));
        }
    }

    public void validateNewsMarketCap(String marketCap) {
        if (marketCap == null || marketCap.trim().isEmpty()) {
            throw new ValidationException("뉴스 시가총액은 필수입니다", List.of("marketCap"));
        }
        if (!marketCap.matches("^(LARGE|MID|SMALL)$")) {
            throw new ValidationException("뉴스 시가총액은 LARGE, MID, SMALL 중 하나여야 합니다", List.of("marketCap"));
        }
    }

    public void validateNewsSector(String sector) {
        if (sector == null || sector.trim().isEmpty()) {
            throw new ValidationException("뉴스 섹터는 필수입니다", List.of("sector"));
        }
        if (sector.length() > 100) {
            throw new ValidationException("뉴스 섹터는 100자 이하여야 합니다", List.of("sector"));
        }
    }

    public void validateNewsIndustry(String industry) {
        if (industry == null || industry.trim().isEmpty()) {
            throw new ValidationException("뉴스 산업은 필수입니다", List.of("industry"));
        }
        if (industry.length() > 100) {
            throw new ValidationException("뉴스 산업은 100자 이하여야 합니다", List.of("industry"));
        }
    }

    public void validateNewsCountry(String country) {
        if (country == null || country.trim().isEmpty()) {
            throw new ValidationException("뉴스 국가는 필수입니다", List.of("country"));
        }
        if (country.length() > 100) {
            throw new ValidationException("뉴스 국가는 100자 이하여야 합니다", List.of("country"));
        }
    }

    public void validateNewsRegion(String region) {
        if (region == null || region.trim().isEmpty()) {
            throw new ValidationException("뉴스 지역은 필수입니다", List.of("region"));
        }
        if (region.length() > 100) {
            throw new ValidationException("뉴스 지역은 100자 이하여야 합니다", List.of("region"));
        }
    }

    public void validateNewsLanguage(String language) {
        if (language == null || language.trim().isEmpty()) {
            throw new ValidationException("뉴스 언어는 필수입니다", List.of("language"));
        }
        if (language.length() > 10) {
            throw new ValidationException("뉴스 언어는 10자 이하여야 합니다", List.of("language"));
        }
    }

    public void validateNewsCurrency(String currency) {
        if (currency == null || currency.trim().isEmpty()) {
            throw new ValidationException("뉴스 통화는 필수입니다", List.of("currency"));
        }
        if (currency.length() > 10) {
            throw new ValidationException("뉴스 통화는 10자 이하여야 합니다", List.of("currency"));
        }
    }

    public void validateNewsExchange(String exchange) {
        if (exchange == null || exchange.trim().isEmpty()) {
            throw new ValidationException("뉴스 거래소는 필수입니다", List.of("exchange"));
        }
        if (exchange.length() > 100) {
            throw new ValidationException("뉴스 거래소는 100자 이하여야 합니다", List.of("exchange"));
        }
    }

    public void validateNewsTicker(String ticker) {
        if (ticker == null || ticker.trim().isEmpty()) {
            throw new ValidationException("뉴스 티커는 필수입니다", List.of("ticker"));
        }
        if (ticker.length() > 20) {
            throw new ValidationException("뉴스 티커는 20자 이하여야 합니다", List.of("ticker"));
        }
    }

    public void validateNewsPrice(double price) {
        if (price < 0) {
            throw new ValidationException("뉴스 가격은 0 이상이어야 합니다", List.of("price"));
        }
    }

    public void validateNewsVolume(long volume) {
        if (volume < 0) {
            throw new ValidationException("뉴스 거래량은 0 이상이어야 합니다", List.of("volume"));
        }
    }

    public void validateNewsMarketValue(double marketValue) {
        if (marketValue < 0) {
            throw new ValidationException("뉴스 시가총액은 0 이상이어야 합니다", List.of("marketValue"));
        }
    }

    public void validateNewsPEScore(double peScore) {
        if (peScore < 0) {
            throw new ValidationException("뉴스 PER은 0 이상이어야 합니다", List.of("peScore"));
        }
    }

    public void validateNewsPBScore(double pbScore) {
        if (pbScore < 0) {
            throw new ValidationException("뉴스 PBR은 0 이상이어야 합니다", List.of("pbScore"));
        }
    }

    public void validateNewsEVScore(double evScore) {
        if (evScore < 0) {
            throw new ValidationException("뉴스 EV는 0 이상이어야 합니다", List.of("evScore"));
        }
    }

    public void validateNewsDebtRatio(double debtRatio) {
        if (debtRatio < 0 || debtRatio > 1) {
            throw new ValidationException("뉴스 부채비율은 0과 1 사이여야 합니다", List.of("debtRatio"));
        }
    }

    public void validateNewsROE(double roe) {
        if (roe < -1 || roe > 1) {
            throw new ValidationException("뉴스 ROE는 -1과 1 사이여야 합니다", List.of("roe"));
        }
    }

    public void validateNewsROA(double roa) {
        if (roa < -1 || roa > 1) {
            throw new ValidationException("뉴스 ROA는 -1과 1 사이여야 합니다", List.of("roa"));
        }
    }

    public void validateNewsGrowthRate(double growthRate) {
        if (growthRate < -1 || growthRate > 1) {
            throw new ValidationException("뉴스 성장률은 -1과 1 사이여야 합니다", List.of("growthRate"));
        }
    }

    public void validateNewsDividendYield(double dividendYield) {
        if (dividendYield < 0 || dividendYield > 1) {
            throw new ValidationException("뉴스 배당수익률은 0과 1 사이여야 합니다", List.of("dividendYield"));
        }
    }

    public void validateNewsBeta(double beta) {
        if (beta < 0) {
            throw new ValidationException("뉴스 베타는 0 이상이어야 합니다", List.of("beta"));
        }
    }

    public void validateNewsAlpha(double alpha) {
        if (alpha < -1 || alpha > 1) {
            throw new ValidationException("뉴스 알파는 -1과 1 사이여야 합니다", List.of("alpha"));
        }
    }

    public void validateNewsSharpeRatio(double sharpeRatio) {
        if (sharpeRatio < -1 || sharpeRatio > 1) {
            throw new ValidationException("뉴스 샤프 비율은 -1과 1 사이여야 합니다", List.of("sharpeRatio"));
        }
    }

    public void validateNewsSortinoRatio(double sortinoRatio) {
        if (sortinoRatio < -1 || sortinoRatio > 1) {
            throw new ValidationException("뉴스 소르티노 비율은 -1과 1 사이여야 합니다", List.of("sortinoRatio"));
        }
    }

    public void validateNewsCalmarRatio(double calmarRatio) {
        if (calmarRatio < -1 || calmarRatio > 1) {
            throw new ValidationException("뉴스 칼마 비율은 -1과 1 사이여야 합니다", List.of("calmarRatio"));
        }
    }

    public void validateNewsInformationRatio(double informationRatio) {
        if (informationRatio < -1 || informationRatio > 1) {
            throw new ValidationException("뉴스 정보 비율은 -1과 1 사이여야 합니다", List.of("informationRatio"));
        }
    }

    public void validateNewsTrackingError(double trackingError) {
        if (trackingError < 0) {
            throw new ValidationException("뉴스 추적 오차는 0 이상이어야 합니다", List.of("trackingError"));
        }
    }

    public void validateNewsMaxDrawdown(double maxDrawdown) {
        if (maxDrawdown < 0 || maxDrawdown > 1) {
            throw new ValidationException("뉴스 최대 낙폭은 0과 1 사이여야 합니다", List.of("maxDrawdown"));
        }
    }

    public void validateNewsVaR(double var) {
        if (var < 0 || var > 1) {
            throw new ValidationException("뉴스 VaR은 0과 1 사이여야 합니다", List.of("var"));
        }
    }

    public void validateNewsCVaR(double cvar) {
        if (cvar < 0 || cvar > 1) {
            throw new ValidationException("뉴스 CVaR은 0과 1 사이여야 합니다", List.of("cvar"));
        }
    }

    public void validateNewsES(double es) {
        if (es < 0 || es > 1) {
            throw new ValidationException("뉴스 ES는 0과 1 사이여야 합니다", List.of("es"));
        }
    }

    public void validateNewsExpectedReturn(double expectedReturn) {
        if (expectedReturn < -1 || expectedReturn > 1) {
            throw new ValidationException("뉴스 기대 수익률은 -1과 1 사이여야 합니다", List.of("expectedReturn"));
        }
    }

    public void validateNewsVolatility(double volatility) {
        if (volatility < 0 || volatility > 1) {
            throw new ValidationException("뉴스 변동성은 0과 1 사이여야 합니다", List.of("volatility"));
        }
    }

    public void validateNewsSkewness(double skewness) {
        if (skewness < -3 || skewness > 3) {
            throw new ValidationException("뉴스 왜도는 -3과 3 사이여야 합니다", List.of("skewness"));
        }
    }

    public void validateNewsKurtosis(double kurtosis) {
        if (kurtosis < -3 || kurtosis > 3) {
            throw new ValidationException("뉴스 첨도는 -3과 3 사이여야 합니다", List.of("kurtosis"));
        }
    }

    public void validateNewsJarqueBera(double jarqueBera) {
        if (jarqueBera < 0) {
            throw new ValidationException("뉴스 Jarque-Bera는 0 이상이어야 합니다", List.of("jarqueBera"));
        }
    }

    public void validateNewsShapiroWilk(double shapiroWilk) {
        if (shapiroWilk < 0 || shapiroWilk > 1) {
            throw new ValidationException("뉴스 Shapiro-Wilk는 0과 1 사이여야 합니다", List.of("shapiroWilk"));
        }
    }

    public void validateNewsKolmogorovSmirnov(double kolmogorovSmirnov) {
        if (kolmogorovSmirnov < 0 || kolmogorovSmirnov > 1) {
            throw new ValidationException("뉴스 Kolmogorov-Smirnov는 0과 1 사이여야 합니다", List.of("kolmogorovSmirnov"));
        }
    }

    public void validateNewsAndersonDarling(double andersonDarling) {
        if (andersonDarling < 0) {
            throw new ValidationException("뉴스 Anderson-Darling은 0 이상이어야 합니다", List.of("andersonDarling"));
        }
    }

    public void validateNewsCramerVonMises(double cramerVonMises) {
        if (cramerVonMises < 0) {
            throw new ValidationException("뉴스 Cramer-von Mises는 0 이상이어야 합니다", List.of("cramerVonMises"));
        }
    }

    public void validateNewsKuiper(double kuiper) {
        if (kuiper < 0) {
            throw new ValidationException("뉴스 Kuiper는 0 이상이어야 합니다", List.of("kuiper"));
        }
    }

    public void validateNewsWatson(double watson) {
        if (watson < 0) {
            throw new ValidationException("뉴스 Watson은 0 이상이어야 합니다", List.of("watson"));
        }
    }

    public void validateNewsDurbinWatson(double durbinWatson) {
        if (durbinWatson < 0 || durbinWatson > 4) {
            throw new ValidationException("뉴스 Durbin-Watson은 0과 4 사이여야 합니다", List.of("durbinWatson"));
        }
    }

    public void validateNewsBreuschGodfrey(double breuschGodfrey) {
        if (breuschGodfrey < 0) {
            throw new ValidationException("뉴스 Breusch-Godfrey는 0 이상이어야 합니다", List.of("breuschGodfrey"));
        }
    }

    public void validateNewsWhite(double white) {
        if (white < 0) {
            throw new ValidationException("뉴스 White는 0 이상이어야 합니다", List.of("white"));
        }
    }

    public void validateNewsHeteroscedasticity(double heteroscedasticity) {
        if (heteroscedasticity < 0 || heteroscedasticity > 1) {
            throw new ValidationException("뉴스 이분산성은 0과 1 사이여야 합니다", List.of("heteroscedasticity"));
        }
    }

    public void validateNewsAutocorrelation(double autocorrelation) {
        if (autocorrelation < -1 || autocorrelation > 1) {
            throw new ValidationException("뉴스 자기상관은 -1과 1 사이여야 합니다", List.of("autocorrelation"));
        }
    }

    public void validateNewsPartialAutocorrelation(double partialAutocorrelation) {
        if (partialAutocorrelation < -1 || partialAutocorrelation > 1) {
            throw new ValidationException("뉴스 부분자기상관은 -1과 1 사이여야 합니다", List.of("partialAutocorrelation"));
        }
    }

    public void validateNewsCrossCorrelation(double crossCorrelation) {
        if (crossCorrelation < -1 || crossCorrelation > 1) {
            throw new ValidationException("뉴스 교차상관은 -1과 1 사이여야 합니다", List.of("crossCorrelation"));
        }
    }

    public void validateNewsCoherence(double coherence) {
        if (coherence < 0 || coherence > 1) {
            throw new ValidationException("뉴스 일관성은 0과 1 사이여야 합니다", List.of("coherence"));
        }
    }

    public void validateNewsPhase(double phase) {
        if (phase < -Math.PI || phase > Math.PI) {
            throw new ValidationException("뉴스 위상은 -π와 π 사이여야 합니다", List.of("phase"));
        }
    }

    public void validateNewsAmplitude(double amplitude) {
        if (amplitude < 0) {
            throw new ValidationException("뉴스 진폭은 0 이상이어야 합니다", List.of("amplitude"));
        }
    }

    public void validateNewsFrequency(double frequency) {
        if (frequency < 0) {
            throw new ValidationException("뉴스 주파수는 0 이상이어야 합니다", List.of("frequency"));
        }
    }

    public void validateNewsPeriod(double period) {
        if (period < 0) {
            throw new ValidationException("뉴스 주기는 0 이상이어야 합니다", List.of("period"));
        }
    }

    public void validateNewsWavelength(double wavelength) {
        if (wavelength < 0) {
            throw new ValidationException("뉴스 파장은 0 이상이어야 합니다", List.of("wavelength"));
        }
    }

    public void validateNewsWavenumber(double wavenumber) {
        if (wavenumber < 0) {
            throw new ValidationException("뉴스 파수는 0 이상이어야 합니다", List.of("wavenumber"));
        }
    }

    public void validateNewsAngularFrequency(double angularFrequency) {
        if (angularFrequency < 0) {
            throw new ValidationException("뉴스 각주파수는 0 이상이어야 합니다", List.of("angularFrequency"));
        }
    }

    public void validateNewsAngularVelocity(double angularVelocity) {
        if (angularVelocity < 0) {
            throw new ValidationException("뉴스 각속도는 0 이상이어야 합니다", List.of("angularVelocity"));
        }
    }

    public void validateNewsAngularAcceleration(double angularAcceleration) {
        if (angularAcceleration < 0) {
            throw new ValidationException("뉴스 각가속도는 0 이상이어야 합니다", List.of("angularAcceleration"));
        }
    }

    public void validateNewsAngularMomentum(double angularMomentum) {
        if (angularMomentum < 0) {
            throw new ValidationException("뉴스 각운동량은 0 이상이어야 합니다", List.of("angularMomentum"));
        }
    }

    public void validateNewsAngularImpulse(double angularImpulse) {
        if (angularImpulse < 0) {
            throw new ValidationException("뉴스 각충격은 0 이상이어야 합니다", List.of("angularImpulse"));
        }
    }

    public void validateNewsAngularWork(double angularWork) {
        if (angularWork < 0) {
            throw new ValidationException("뉴스 각일은 0 이상이어야 합니다", List.of("angularWork"));
        }
    }

    public void validateNewsAngularPower(double angularPower) {
        if (angularPower < 0) {
            throw new ValidationException("뉴스 각파워는 0 이상이어야 합니다", List.of("angularPower"));
        }
    }

    public void validateNewsAngularEnergy(double angularEnergy) {
        if (angularEnergy < 0) {
            throw new ValidationException("뉴스 각에너지는 0 이상이어야 합니다", List.of("angularEnergy"));
        }
    }

    public void validateNewsAngularForce(double angularForce) {
        if (angularForce < 0) {
            throw new ValidationException("뉴스 각력은 0 이상이어야 합니다", List.of("angularForce"));
        }
    }

    public void validateNewsAngularTorque(double angularTorque) {
        if (angularTorque < 0) {
            throw new ValidationException("뉴스 각토크는 0 이상이어야 합니다", List.of("angularTorque"));
        }
    }

    public void validateNewsAngularPeriod(double angularPeriod) {
        if (angularPeriod < 0) {
            throw new ValidationException("뉴스 각주기는 0 이상이어야 합니다", List.of("angularPeriod"));
        }
    }

    public void validateNewsAngularWavelength(double angularWavelength) {
        if (angularWavelength < 0) {
            throw new ValidationException("뉴스 각파장은 0 이상이어야 합니다", List.of("angularWavelength"));
        }
    }

    public void validateNewsAngularWavenumber(double angularWavenumber) {
        if (angularWavenumber < 0) {
            throw new ValidationException("뉴스 각파수는 0 이상이어야 합니다", List.of("angularWavenumber"));
        }
    }

    public void validateNewsAngularPhase(double angularPhase) {
        if (angularPhase < -Math.PI || angularPhase > Math.PI) {
            throw new ValidationException("뉴스 각위상은 -π와 π 사이여야 합니다", List.of("angularPhase"));
        }
    }

    public void validateNewsAngularAmplitude(double angularAmplitude) {
        if (angularAmplitude < 0) {
            throw new ValidationException("뉴스 각진폭은 0 이상이어야 합니다", List.of("angularAmplitude"));
        }
    }

    public void validateNewsAngularCoherence(double angularCoherence) {
        if (angularCoherence < 0 || angularCoherence > 1) {
            throw new ValidationException("뉴스 각일관성은 0과 1 사이여야 합니다", List.of("angularCoherence"));
        }
    }

    public void validateNewsAngularCorrelation(double angularCorrelation) {
        if (angularCorrelation < -1 || angularCorrelation > 1) {
            throw new ValidationException("뉴스 각상관은 -1과 1 사이여야 합니다", List.of("angularCorrelation"));
        }
    }

    public void validateNewsAngularCrossCorrelation(double angularCrossCorrelation) {
        if (angularCrossCorrelation < -1 || angularCrossCorrelation > 1) {
            throw new ValidationException("뉴스 각교차상관은 -1과 1 사이여야 합니다", List.of("angularCrossCorrelation"));
        }
    }

    public void validateNewsAngularPartialCorrelation(double angularPartialCorrelation) {
        if (angularPartialCorrelation < -1 || angularPartialCorrelation > 1) {
            throw new ValidationException("뉴스 각부분상관은 -1과 1 사이여야 합니다", List.of("angularPartialCorrelation"));
        }
    }

    public void validateNewsAngularAutocorrelation(double angularAutocorrelation) {
        if (angularAutocorrelation < -1 || angularAutocorrelation > 1) {
            throw new ValidationException("뉴스 각자기상관은 -1과 1 사이여야 합니다", List.of("angularAutocorrelation"));
        }
    }

    public void validateNewsAngularHeteroscedasticity(double angularHeteroscedasticity) {
        if (angularHeteroscedasticity < 0 || angularHeteroscedasticity > 1) {
            throw new ValidationException("뉴스 각이분산성은 0과 1 사이여야 합니다", List.of("angularHeteroscedasticity"));
        }
    }

    public void validateNewsAngularWhite(double angularWhite) {
        if (angularWhite < 0) {
            throw new ValidationException("뉴스 각화이트는 0 이상이어야 합니다", List.of("angularWhite"));
        }
    }

    public void validateNewsAngularBreuschGodfrey(double angularBreuschGodfrey) {
        if (angularBreuschGodfrey < 0) {
            throw new ValidationException("뉴스 각브로이슈-고드프리는 0 이상이어야 합니다", List.of("angularBreuschGodfrey"));
        }
    }

    public void validateNewsAngularDurbinWatson(double angularDurbinWatson) {
        if (angularDurbinWatson < 0 || angularDurbinWatson > 4) {
            throw new ValidationException("뉴스 각더빈-왓슨은 0과 4 사이여야 합니다", List.of("angularDurbinWatson"));
        }
    }

    public void validateNewsAngularKuiper(double angularKuiper) {
        if (angularKuiper < 0) {
            throw new ValidationException("뉴스 각쿠이퍼는 0 이상이어야 합니다", List.of("angularKuiper"));
        }
    }

    public void validateNewsAngularWatson(double angularWatson) {
        if (angularWatson < 0) {
            throw new ValidationException("뉴스 각왓슨은 0 이상이어야 합니다", List.of("angularWatson"));
        }
    }

    public void validateNewsAngularCramerVonMises(double angularCramerVonMises) {
        if (angularCramerVonMises < 0) {
            throw new ValidationException("뉴스 각크라머-폰 미제스는 0 이상이어야 합니다", List.of("angularCramerVonMises"));
        }
    }

    public void validateNewsAngularAndersonDarling(double angularAndersonDarling) {
        if (angularAndersonDarling < 0) {
            throw new ValidationException("뉴스 각앤더슨-달링은 0 이상이어야 합니다", List.of("angularAndersonDarling"));
        }
    }

    public void validateNewsAngularKolmogorovSmirnov(double angularKolmogorovSmirnov) {
        if (angularKolmogorovSmirnov < 0 || angularKolmogorovSmirnov > 1) {
            throw new ValidationException("뉴스 각콜모고로프-스미르노프는 0과 1 사이여야 합니다", List.of("angularKolmogorovSmirnov"));
        }
    }

    public void validateNewsAngularShapiroWilk(double angularShapiroWilk) {
        if (angularShapiroWilk < 0 || angularShapiroWilk > 1) {
            throw new ValidationException("뉴스 각샤피로-윌크는 0과 1 사이여야 합니다", List.of("angularShapiroWilk"));
        }
    }

    public void validateNewsAngularJarqueBera(double angularJarqueBera) {
        if (angularJarqueBera < 0) {
            throw new ValidationException("뉴스 각자크-베라는 0 이상이어야 합니다", List.of("angularJarqueBera"));
        }
    }

    public void validateNewsAngularKurtosis(double angularKurtosis) {
        if (angularKurtosis < -3 || angularKurtosis > 3) {
            throw new ValidationException("뉴스 각첨도는 -3과 3 사이여야 합니다", List.of("angularKurtosis"));
        }
    }

    public void validateNewsAngularSkewness(double angularSkewness) {
        if (angularSkewness < -3 || angularSkewness > 3) {
            throw new ValidationException("뉴스 각왜도는 -3과 3 사이여야 합니다", List.of("angularSkewness"));
        }
    }

    public void validateNewsAngularVolatility(double angularVolatility) {
        if (angularVolatility < 0 || angularVolatility > 1) {
            throw new ValidationException("뉴스 각변동성은 0과 1 사이여야 합니다", List.of("angularVolatility"));
        }
    }

    public void validateNewsAngularExpectedReturn(double angularExpectedReturn) {
        if (angularExpectedReturn < -1 || angularExpectedReturn > 1) {
            throw new ValidationException("뉴스 각기대수익률은 -1과 1 사이여야 합니다", List.of("angularExpectedReturn"));
        }
    }

    public void validateNewsAngularES(double angularES) {
        if (angularES < 0 || angularES > 1) {
            throw new ValidationException("뉴스 각ES는 0과 1 사이여야 합니다", List.of("angularES"));
        }
    }

    public void validateNewsAngularCVaR(double angularCVaR) {
        if (angularCVaR < 0 || angularCVaR > 1) {
            throw new ValidationException("뉴스 각CVaR은 0과 1 사이여야 합니다", List.of("angularCVaR"));
        }
    }

    public void validateNewsAngularVaR(double angularVaR) {
        if (angularVaR < 0 || angularVaR > 1) {
            throw new ValidationException("뉴스 각VaR은 0과 1 사이여야 합니다", List.of("angularVaR"));
        }
    }
}