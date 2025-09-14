package com.ygk.mcp.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 火车票查询工具
 * 提供火车票余量查询功能的MCP工具
 */
public class QueryTicketTool {

    private final DatabaseClient dbClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QueryTicketTool(DatabaseClient dbClient) {
        this.dbClient = dbClient;
    }

    public McpServerFeatures.AsyncToolSpecification getToolSpec() {

        String querySchema = """
            {
              "type": "object",
              "properties": {
                "startStation": { "type": "string" },
                "endStation": { "type": "string" }
              },
              "required": ["startStation","endStation"]
            }
            """;

        return new McpServerFeatures.AsyncToolSpecification(
                new McpSchema.Tool("query-ticket", "Query high-speed train tickets", querySchema),
                (exchange, arguments) -> {
                    String start = (String) arguments.get("startStation");
                    String end = (String) arguments.get("endStation");

                    String safeStart = sanitize(start);
                    String safeEnd = sanitize(end);

                    String sql = "SELECT t.train_type, t.train_number, s.seat_type, MIN(s.price) AS min_price, COUNT(*) AS remaining " +
                            "FROM t_seat s " +
                            "INNER JOIN t_train t ON s.train_id = t.id " +
                            "WHERE s.start_station = '" + safeStart + "' AND s.end_station = '" + safeEnd + "' AND s.seat_status = 0 " +
                            "GROUP BY t.train_type, t.train_number, s.seat_type";

                    return dbClient.sql(sql)
                            .map((row, meta) -> Map.of(
                                    "trainType", row.get("train_type"),
                                    "trainNumber", row.get("train_number"),
                                    "seatType", row.get("seat_type"),
                                    "minPrice", row.get("min_price"),
                                    "remaining", row.get("remaining")
                            ))
                            .all()
                            .collectList()
                            .timeout(Duration.ofSeconds(60))
                            .map(rows -> {
                                Map<String, TrainAggregation> trainAggMap = new HashMap<>();

                                for (Map<String, Object> r : rows) {
                                    Integer trainType = r.get("trainType") instanceof Number ? ((Number) r.get("trainType")).intValue() : null;
                                    String trainNumber = r.get("trainNumber") != null ? r.get("trainNumber").toString() : "";
                                    Integer minPrice = r.get("minPrice") instanceof Number ? ((Number) r.get("minPrice")).intValue() : null;
                                    Integer remaining = r.get("remaining") instanceof Number ? ((Number) r.get("remaining")).intValue() : null;
                                    if (trainType == null || trainNumber.isEmpty() || minPrice == null || remaining == null) continue;

                                    TrainAggregation agg = trainAggMap.computeIfAbsent(trainNumber, k -> new TrainAggregation(trainType, trainNumber));
                                    agg.totalRemaining += remaining;
                                    agg.priceToRemaining.merge(minPrice, remaining, Integer::sum);
                                }

                                if (trainAggMap.isEmpty()) {
                                    return emptyResult();
                                }

                                TrainAggregation top = trainAggMap.values().stream()
                                        .max(Comparator.comparingInt(a -> a.totalRemaining))
                                        .orElseGet(() -> trainAggMap.values().iterator().next());

                                List<Map.Entry<Integer, Integer>> ranked = new ArrayList<>(top.priceToRemaining.entrySet());
                                ranked.sort((a, b) -> Integer.compare(b.getKey(), a.getKey()));

                                String[] levelOrder = getLevelOrderByTrainType(top.trainType);
                                Map<String, Object> summary = new LinkedHashMap<>();
                                int idx = 0;
                                for (Map.Entry<Integer, Integer> e : ranked) {
                                    if (idx >= levelOrder.length) break;
                                    String levelName = levelOrder[idx];
                                    Integer price = e.getKey();
                                    Integer remain = e.getValue();

                                    Map<String, Object> levelInfo = new LinkedHashMap<>();
                                    levelInfo.put("remaining", remain);
                                    levelInfo.put("price", price + "元");
                                    levelInfo.put("trainNumber", top.trainNumber);
                                    levelInfo.put("trainType", getTrainTypeName(top.trainType));
                                    summary.put(levelName, levelInfo);
                                    idx++;
                                }
                                summary.put("totalRemaining", top.totalRemaining);

                                try {
                                    String json = objectMapper.writeValueAsString(summary);
                                    List<McpSchema.Content> contents = new ArrayList<>();
                                    contents.add(new McpSchema.TextContent(json));
                                    return new McpSchema.CallToolResult(contents, false);
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .onErrorResume(ex -> {
                                try {
                                    String json = objectMapper.writeValueAsString(Map.of(
                                            "success", false,
                                            "message", "查询失败: " + ex.getMessage()
                                    ));
                                    List<McpSchema.Content> contents = new ArrayList<>();
                                    contents.add(new McpSchema.TextContent(json));
                                    return Mono.just(new McpSchema.CallToolResult(contents, true));
                                } catch (Exception e) {
                                    return Mono.just(new McpSchema.CallToolResult(List.of(new McpSchema.TextContent("查询失败")), true));
                                }
                            });
                }
        );
    }

    private static class TrainAggregation {
        final int trainType;
        final String trainNumber;
        int totalRemaining = 0;
        Map<Integer, Integer> priceToRemaining = new HashMap<>();

        TrainAggregation(int trainType, String trainNumber) {
            this.trainType = trainType;
            this.trainNumber = trainNumber;
        }
    }

    private String[] getLevelOrderByTrainType(int trainType) {
        return switch (trainType) {
            case 0 -> new String[]{"商务座", "一等座", "二等座"};
            case 1 -> new String[]{"二等包座", "一等卧", "二等卧", "无座站票"};
            case 2 -> new String[]{"软卧", "硬卧", "硬座", "无座"};
            default -> new String[]{"座位一", "座位二", "座位三"};
        };
    }

    private String getTrainTypeName(Integer trainType) {
        if (trainType == null) return "";
        return switch (trainType) {
            case 0 -> "高铁";
            case 1 -> "动车";
            case 2 -> "普通车";
            default -> "未知类型";
        };
    }

    private McpSchema.CallToolResult emptyResult() {
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "totalRemaining", 0
            ));
            return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(json)), false);
        } catch (JsonProcessingException e) {
            return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent("{}")), false);
        }
    }

    private String sanitize(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.length() > 100) t = t.substring(0, 100);
        return t.replace("'", "''");
    }
}
