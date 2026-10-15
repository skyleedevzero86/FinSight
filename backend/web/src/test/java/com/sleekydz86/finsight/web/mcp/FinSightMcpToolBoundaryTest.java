package com.sleekydz86.finsight.web.mcp;

import com.sleekydz86.finsight.web.mcp.tools.AccountMcpTools;
import com.sleekydz86.finsight.web.mcp.tools.AdminMcpTools;
import com.sleekydz86.finsight.web.mcp.tools.BoardMcpTools;
import com.sleekydz86.finsight.web.mcp.tools.LiveVodMcpTools;
import com.sleekydz86.finsight.web.mcp.tools.NewsCrawlIncidentTools;
import com.sleekydz86.finsight.web.mcp.tools.NewsMcpTools;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class FinSightMcpToolBoundaryTest {

    @Test
    void 재수집_도구는_공개_MCP_제공자에_등록되지_않는다() throws Exception {
        Method method = FinSightMcpConfiguration.class.getDeclaredMethod(
                "finSightMcpTools",
                NewsMcpTools.class,
                NewsCrawlIncidentTools.class,
                BoardMcpTools.class,
                LiveVodMcpTools.class,
                AccountMcpTools.class,
                AdminMcpTools.class);
        assertThat(Arrays.stream(method.getParameterTypes()).map(Class::getSimpleName))
                .contains("NewsCrawlIncidentTools")
                .doesNotContain("NewsCrawlRecrawlTool");
    }
}
