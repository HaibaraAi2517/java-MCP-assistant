package com.ygk.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.r2dbc.core.DatabaseClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryTicketTool {

    private final DatabaseClient dbClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QueryTicketTool(DatabaseClient dbClient) {
        this.dbClient = dbClient;
    }

    public McpServerFeatures.AsyncToolSpecification getToolSpec() {

        // 请求参数 JSON Schema
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

                    // 联表查询：根据 train_type 和 seat_type 确定座位等级，并获取列车信息
                    String sql = "SELECT s.seat_type, s.price, t.train_type, t.train_number " +
                            "FROM t_seat s " +
                            "INNER JOIN t_train t ON s.train_id = t.id " +
                            "WHERE s.start_station = '" + start + "' AND s.end_station = '" + end + "' AND s.seat_status = 0";

                    return dbClient.sql(sql)
                            .map(row -> Map.of(
                                    "seatType", row.get("seat_type"),
                                    "price", row.get("price"),
                                    "trainType", row.get("train_type"),
                                    "trainNumber", row.get("train_number")
                            ))
                            .all()
                            .collectList()
                            .map(rows -> {
                                // 按座位等级统计余票与最低价格，并收集列车信息
                                Map<String, Integer> counts = new HashMap<>();
                                Map<String, Integer> minPrices = new HashMap<>();
                                Map<String, String> trainNumbers = new HashMap<>();
                                Map<String, String> trainTypes = new HashMap<>();

                                for (Map<String, Object> r : rows) {
                                    Object seatTypeObj = r.get("seatType");
                                    Object priceObj = r.get("price");
                                    Object trainTypeObj = r.get("trainType");
                                    Object trainNumberObj = r.get("trainNumber");
                                    
                                    Integer seatType = null;
                                    Integer price = null;
                                    Integer trainType = null;
                                    String trainNumber = trainNumberObj != null ? trainNumberObj.toString() : "";
                                    
                                    if (seatTypeObj instanceof Number) {
                                        seatType = ((Number) seatTypeObj).intValue();
                                    }
                                    if (priceObj instanceof Number) {
                                        price = ((Number) priceObj).intValue();
                                    }
                                    if (trainTypeObj instanceof Number) {
                                        trainType = ((Number) trainTypeObj).intValue();
                                    }

                                    if (seatType == null || trainType == null) continue;

                                    String level = getSeatLevel(trainType, seatType);
                                    if (level == null) continue;

                                    counts.put(level, counts.getOrDefault(level, 0) + 1);
                                    if (price != null) {
                                        minPrices.put(level, Math.min(minPrices.getOrDefault(level, Integer.MAX_VALUE), price));
                                    }
                                    
                                    // 收集列车信息（取第一个遇到的列车信息）
                                    if (!trainNumbers.containsKey(level)) {
                                        trainNumbers.put(level, trainNumber);
                                        trainTypes.put(level, getTrainTypeName(trainType));
                                    }
                                }

                                // 构建结果（价格显示为元，并添加单位说明，包含列车信息）
                                Map<String, Object> summary = new HashMap<>();
                                for (String level : counts.keySet()) {
                                    Integer price = minPrices.getOrDefault(level, 0);
                                    String trainNumber = trainNumbers.getOrDefault(level, "");
                                    String trainType = trainTypes.getOrDefault(level, "");
                                    
                                    Map<String, Object> levelInfo = new HashMap<>();
                                    levelInfo.put("remaining", counts.get(level));
                                    levelInfo.put("price", price + "元");
                                    levelInfo.put("trainNumber", trainNumber);
                                    levelInfo.put("trainType", trainType);
                                    
                                    summary.put(level, levelInfo);
                                }
                                summary.put("totalRemaining", counts.values().stream().mapToInt(Integer::intValue).sum());

                                try {
                                    String json = objectMapper.writeValueAsString(summary);
                                    List<McpSchema.Content> contents = new ArrayList<>();
                                    contents.add(new McpSchema.TextContent(json));
                                    return new McpSchema.CallToolResult(contents, false);
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                }
        );
    }

    private String getSeatLevel(Integer trainType, Integer seatType) {
        if (trainType == null || seatType == null) return null;

        switch (trainType) {
            case 0: // 高铁
                return switch (seatType) {
                    case 0 -> "商务座";
                    case 1 -> "一等座";
                    case 2 -> "二等座";
                    default -> null;
                };
            case 1: // 动车
                return switch (seatType) {
                    case 0 -> "二等包座";
                    case 1 -> "一等卧";
                    case 4 -> "二等卧";
                    case 5 -> "无座站票";
                    default -> null;
                };
            case 2: // 普通车
                return switch (seatType) {
                    case 1 -> "软卧";
                    case 2 -> "硬卧";
                    case 3 -> "硬座";
                    case 4 -> "无座";
                    default -> null;
                };
            default:
                return null;
        }
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
}
