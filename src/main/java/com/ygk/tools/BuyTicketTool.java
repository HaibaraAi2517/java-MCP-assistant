package com.ygk.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BuyTicketTool {

    private final DatabaseClient dbClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BuyTicketTool(DatabaseClient dbClient) {
        this.dbClient = dbClient;
    }

    public McpServerFeatures.AsyncToolSpecification getToolSpec() {

        String buySchema = """
            {
              "type": "object",
              "properties": {
                "ticketId": { "type": "number" }
              },
              "required": ["ticketId"]
            }
            """;

        return new McpServerFeatures.AsyncToolSpecification(
                new McpSchema.Tool("buy-ticket", "Purchase high-speed train ticket", buySchema),
                (exchange, arguments) -> {
                    Long ticketId = ((Number) arguments.get("ticketId")).longValue();

                    String sql = "UPDATE t_seat SET seat_status = 1 WHERE id = " + ticketId + " AND seat_status = 0";

                    return Mono.from(
                            dbClient.sql(sql)
                                    .fetch()
                                    .rowsUpdated()
                                    .map(count -> {
                                        String message;
                                        if (count > 0) {
                                            message = "Purchase successful";
                                        } else {
                                            message = "Ticket already sold or invalid";
                                        }

                                        // 将字符串封装为 TextContent
                                        List<McpSchema.Content> contents = new ArrayList<>();
                                        try {
                                            String json = objectMapper.writeValueAsString(Map.of("message", message));
                                            contents.add(new McpSchema.TextContent(json));
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }

                                        // 返回 CallToolResult
                                        return new McpSchema.CallToolResult(contents, false);
                                    })
                    );
                }
        );
    }
}
