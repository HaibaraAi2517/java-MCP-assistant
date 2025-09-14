package com.ygk.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 火车票购买工具
 * 提供火车票预订功能的MCP工具
 */
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
                "startStation": { "type": "string" },
                "endStation": { "type": "string" },
                "seatLevel": { "type": "string" },
                "numSeats": { "type": "number" }
              },
              "required": ["startStation", "endStation", "seatLevel", "numSeats"]
            }
            """;

        return new McpServerFeatures.AsyncToolSpecification(
                new McpSchema.Tool("buy-ticket", "Purchase high-speed train ticket", buySchema),
                (exchange, arguments) -> {
                    String startStation = (String) arguments.get("startStation");
                    String endStation = (String) arguments.get("endStation");
                    String seatLevel = (String) arguments.get("seatLevel");
                    Integer numSeats = ((Number) arguments.get("numSeats")).intValue();

                    // 先查询可用的座位ID（包含 seat_type，供筛选）
                    String querySql = "SELECT s.id, s.price, s.seat_type, t.train_type, t.train_number " +
                            "FROM t_seat s " +
                            "INNER JOIN t_train t ON s.train_id = t.id " +
                            "WHERE s.start_station = '" + startStation + "' AND s.end_station = '" + endStation +
                            "' AND s.seat_status = 0";

                    return dbClient.sql(querySql)
                            .map(row -> Map.of(
                                    "id", row.get("id"),
                                    "price", row.get("price"),
                                    "trainType", row.get("train_type"),
                                    "seatType", row.get("seat_type"),
                                    "trainNumber", row.get("train_number")
                            ))
                            .all()
                            .collectList()
                            .flatMap(rows -> {
                                // 根据座位等级筛选并选择座位
                                List<Map<String, Object>> availableSeats = new ArrayList<>();

                                for (Map<String, Object> row : rows) {
                                    Object seatTypeObj = row.get("seatType");
                                    Object trainTypeObj = row.get("trainType");

                                    Integer seatType = null;
                                    Integer trainType = null;

                                    if (seatTypeObj instanceof Number) {
                                        seatType = ((Number) seatTypeObj).intValue();
                                    }
                                    if (trainTypeObj instanceof Number) {
                                        trainType = ((Number) trainTypeObj).intValue();
                                    }

                                    if (seatType == null || trainType == null) continue;

                                    String level = getSeatLevel(trainType, seatType);
                                    if (level != null && level.equals(seatLevel)) {
                                        availableSeats.add(row);
                                    }
                                }

                                if (availableSeats.size() < numSeats) {
                                    // 座位不足
                                    List<McpSchema.Content> contents = new ArrayList<>();
                                    try {
                                        String json = objectMapper.writeValueAsString(Map.of(
                                                "success", false,
                                                "message", "座位不足，可用座位：" + availableSeats.size() + "张，需要：" + numSeats + "张"
                                        ));
                                        contents.add(new McpSchema.TextContent(json));
                                        return Mono.just(new McpSchema.CallToolResult(contents, false));
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                }

                                // 选择前numSeats个座位进行预订
                                List<Long> selectedIds = availableSeats.stream()
                                        .limit(numSeats)
                                        .map(row -> ((Number) row.get("id")).longValue())
                                        .toList();

                                // 批量更新座位状态
                                String updateSql = "UPDATE t_seat SET seat_status = 1 WHERE id IN (" +
                                        selectedIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("") + ")";

                                return Mono.from(
                                        dbClient.sql(updateSql)
                                                .fetch()
                                                .rowsUpdated()
                                                .map(count -> {
                                                    List<McpSchema.Content> contents = new ArrayList<>();
                                                    try {
                                                        String json = objectMapper.writeValueAsString(Map.of(
                                                                "success", true,
                                                                "message", "成功预订 " + count + " 张" + seatLevel + "票",
                                                                "bookedSeats", count,
                                                                "seatIds", selectedIds,
                                                                "totalPrice", availableSeats.stream()
                                                                        .limit(numSeats)
                                                                        .mapToInt(row -> ((Number) row.get("price")).intValue())
                                                                        .sum() + "元"
                                                        ));
                                                        contents.add(new McpSchema.TextContent(json));
                                                        return new McpSchema.CallToolResult(contents, false);
                                                    } catch (Exception e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                })
                                );
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
}
