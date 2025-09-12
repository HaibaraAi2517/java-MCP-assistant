package com.ygk.mcp_config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ygk.tools.BuyTicketTool;
import com.ygk.tools.QueryTicketTool;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.WebMvcSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.servlet.function.RouterFunction;

@Configuration
public class McpServerConfig {

    @Bean
    public WebMvcSseServerTransportProvider transportProvider(ObjectMapper mapper) {
        return new WebMvcSseServerTransportProvider(mapper, "/sse");
    }

    @Bean
    public RouterFunction<?> mcpRouterFunction(WebMvcSseServerTransportProvider transportProvider) {
        return transportProvider.getRouterFunction();
    }

    @Bean
    public McpAsyncServer mcpServer(WebMvcSseServerTransportProvider transportProvider,
                                    DatabaseClient dbClient) {

        McpAsyncServer server = McpServer.async(transportProvider)
                .serverInfo("HighSpeedRailServer", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .resources(false, true)
                        .prompts(false)
                        .logging()
                        .build())
                .build();

        server.addTool(new QueryTicketTool(dbClient).getToolSpec()).subscribe();
        server.addTool(new BuyTicketTool(dbClient).getToolSpec()).subscribe();

        return server;
    }
}
