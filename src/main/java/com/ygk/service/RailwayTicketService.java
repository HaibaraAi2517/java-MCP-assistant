package com.ygk.service;

import com.ygk.bean.TicketBookingResult;
import com.ygk.bean.TicketQueryResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 火车票服务
 * 直接调用 Python MCP 服务器的 HTTP API
 */
@Service
public class RailwayTicketService {

    @Value("${mcp.server.host:localhost}")
    private String mcpServerHost;

    @Value("${mcp.server.port:8000}")
    private int mcpServerPort;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 查询火车票余量
     */
    public TicketQueryResult checkTicket(String startStation, String endStation) {
        try {
            String url = String.format("http://%s:%d/check_ticket_tool", mcpServerHost, mcpServerPort);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("start_station", startStation);
            requestBody.put("end_station", endStation);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response =
                    restTemplate.exchange(url,
                            HttpMethod.POST,
                            request,
                            new ParameterizedTypeReference<Map<String, Object>>() {});


            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = response.getBody();
                String status = (String) result.get("status");
                String message = (String) result.get("message");
                Integer remaining = (Integer) result.get("remaining");
                
                return new TicketQueryResult(status.equals("success"), message, remaining);
            } else {
                return new TicketQueryResult(false, "查询失败", null);
            }
        } catch (Exception e) {
            return new TicketQueryResult(false, "查询异常: " + e.getMessage(), null);
        }
    }

    /**
     * 预订火车票
     */
    public TicketBookingResult bookTicket(String startStation, String endStation, int numSeats) {
        try {
            String url = String.format("http://%s:%d/book_ticket_tool", mcpServerHost, mcpServerPort);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("start_station", startStation);
            requestBody.put("end_station", endStation);
            requestBody.put("num_seats", numSeats);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response =
                    restTemplate.exchange(url,
                            HttpMethod.POST,
                            request,
                            new ParameterizedTypeReference<Map<String, Object>>() {});
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = response.getBody();
                String status = (String) result.get("status");
                String message = (String) result.get("message");
                Integer booked = (Integer) result.get("booked");
                
                return new TicketBookingResult(status.equals("success"), message, booked != null ? booked : 0);
            } else {
                return new TicketBookingResult(false, "订票失败", 0);
            }
        } catch (Exception e) {
            return new TicketBookingResult(false, "订票异常: " + e.getMessage(), 0);
        }
    }
}
