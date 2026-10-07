package com.sleekydz86.finsight.web.mcp;

import com.sleekydz86.finsight.web.mcp.tools.AccountMcpTools;
import com.sleekydz86.finsight.web.mcp.tools.AdminMcpTools;
import com.sleekydz86.finsight.web.mcp.tools.BoardMcpTools;
import com.sleekydz86.finsight.web.mcp.tools.LiveVodMcpTools;
import com.sleekydz86.finsight.web.mcp.tools.NewsMcpTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "spring.ai.mcp.server", name = "enabled", havingValue = "true")
public class FinSightMcpConfiguration {

    @Bean
    public ToolCallbackProvider finSightMcpTools(
            NewsMcpTools newsMcpTools,
            BoardMcpTools boardMcpTools,
            LiveVodMcpTools liveVodMcpTools,
            AccountMcpTools accountMcpTools,
            AdminMcpTools adminMcpTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(newsMcpTools, boardMcpTools, liveVodMcpTools, accountMcpTools, adminMcpTools)
                .build();
    }
}
