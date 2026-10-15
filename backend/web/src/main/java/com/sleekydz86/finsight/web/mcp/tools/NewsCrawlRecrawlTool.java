package com.sleekydz86.finsight.web.mcp.tools;

import com.sleekydz86.finsight.core.news.crawl.NewsCrawlRecoveryAgent;
import com.sleekydz86.finsight.core.news.crawl.NewsCrawlReport;
import com.sleekydz86.finsight.web.mcp.McpCaller;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class NewsCrawlRecrawlTool {

    private final NewsCrawlRecoveryAgent agent;

    public NewsCrawlRecrawlTool(NewsCrawlRecoveryAgent agent) {
        this.agent = agent;
    }

    @Tool(description = "관리자가 승인한 뉴스 수집 장애만 재수집합니다. 공개 조회 도구에 포함되지 않습니다.")
    public NewsCrawlReport recrawlApprovedIncident(
            @ToolParam(description = "승인된 장애 사건 id") long incidentId) {
        return agent.execute(incidentId, McpCaller.requireEmail());
    }
}
