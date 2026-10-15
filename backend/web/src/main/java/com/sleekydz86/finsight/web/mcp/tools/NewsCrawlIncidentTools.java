package com.sleekydz86.finsight.web.mcp.tools;

import com.sleekydz86.finsight.core.news.crawl.NewsCrawlAttempt;
import com.sleekydz86.finsight.core.news.crawl.NewsCrawlIncident;
import com.sleekydz86.finsight.core.news.crawl.NewsCrawlRecoveryAgent;
import com.sleekydz86.finsight.web.mcp.McpCaller;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "spring.ai.mcp.server", name = "enabled", havingValue = "true")
public class NewsCrawlIncidentTools {

    private final NewsCrawlRecoveryAgent agent;

    public NewsCrawlIncidentTools(NewsCrawlRecoveryAgent agent) {
        this.agent = agent;
    }

    @Tool(description = "뉴스 수집 장애 사건 목록을 조회합니다. 읽기 전용이며 SQL을 실행하지 않습니다.")
    public List<NewsCrawlIncident> listNewsCrawlIncidents(
            @ToolParam(description = "조회 개수 (1~50)", required = false) Integer limit) {
        McpCaller.requireAdmin();
        int size = limit == null ? 20 : limit;
        return agent.listIncidents(size);
    }

    @Tool(description = "뉴스 수집 장애 사건과 실행 시도를 조회합니다. 읽기 전용이며 재수집하지 않습니다.")
    public NewsCrawlIncidentView getNewsCrawlIncident(
            @ToolParam(description = "장애 사건 id") long incidentId) {
        McpCaller.requireAdmin();
        return new NewsCrawlIncidentView(agent.incident(incidentId), agent.attempts(incidentId));
    }

    public record NewsCrawlIncidentView(NewsCrawlIncident incident, List<NewsCrawlAttempt> attempts) {
    }
}
