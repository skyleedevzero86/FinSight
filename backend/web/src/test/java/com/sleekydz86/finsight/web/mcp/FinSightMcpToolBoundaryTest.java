package com.sleekydz86.finsight.web.mcp;

import com.sleekydz86.finsight.web.mcp.tools.AccountMcpTools;
import com.sleekydz86.finsight.web.mcp.tools.AdminMcpTools;
import com.sleekydz86.finsight.web.mcp.tools.BoardMcpTools;
import com.sleekydz86.finsight.web.mcp.tools.LiveVodMcpTools;
import com.sleekydz86.finsight.web.mcp.tools.NewsCrawlIncidentTools;
import com.sleekydz86.finsight.web.mcp.tools.NewsMcpTools;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class FinSightMcpToolBoundaryTest {

    @Test
    @DisplayName("재수집 도구는 공개 MCP 제공자에 등록되지 않는다")
    void recrawlToolIsNotRegisteredOnThePublicMcpProvider() throws Exception {
        // given

        // when
        Method method = FinSightMcpConfiguration.class.getDeclaredMethod(
                "finSightMcpTools",
                NewsMcpTools.class,
                NewsCrawlIncidentTools.class,
                BoardMcpTools.class,
                LiveVodMcpTools.class,
                AccountMcpTools.class,
                AdminMcpTools.class);

        // then
        assertThat(Arrays.stream(method.getParameterTypes()).map(Class::getSimpleName))
                .contains("NewsCrawlIncidentTools")
                .doesNotContain("NewsCrawlRecrawlTool");
    }
}
