package com.sleekydz86.finsight.core.news.crawl;

import org.springframework.stereotype.Component;

@Component
public class RuleOnlyNewsCrawlNarrative implements NewsCrawlNarrative {

    @Override
    public String explain(NewsCrawlIncident incident) {
        return "규칙 분류 " + incident.cause() + " 를 유지합니다. 모델 서술은 사용하지 않았습니다.";
    }
}
