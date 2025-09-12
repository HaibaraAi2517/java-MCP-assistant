package com.ygk.client;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class McpClientConfig {

    @Value("${mcp.server.base-url:http://127.0.0.1:8080/sse}")
    private String mcpServerBaseUrl;

    @Value("${mcp.client.log-requests:true}")
    private boolean logRequests;

    @Value("${mcp.client.log-responses:true}")
    private boolean logResponses;

    @Bean
    @Lazy
    public HttpMcpTransport mcpTransport() {
        return new HttpMcpTransport.Builder()
                .sseUrl(mcpServerBaseUrl)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }

    @Bean
    @Lazy
    public McpClient mcpClient(HttpMcpTransport mcpTransport) {
        return new DefaultMcpClient.Builder()
                .transport(mcpTransport)
                .build();
    }

    // Bean name intentionally set to `toolProvider` to match typical agent wiring
    @Bean(name = "toolProvider")
    @Lazy
    public McpToolProvider toolProvider(McpClient mcpClient) {
        return McpToolProvider.builder()
                .mcpClients(java.util.List.of(mcpClient))
                .failIfOneServerFails(false)
                .build();
    }
}
