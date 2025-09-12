package com.ygk.service;

import com.ygk.bean.TicketBookingResult;
import com.ygk.bean.TicketQueryResult;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 火车票工具提供者
 * 为 AI 助手提供火车票查询和预订功能
 */
@Component
public class RailwayTicketToolProvider {

    @Autowired
    private RailwayTicketService railwayTicketService;

    @Tool("查询两个城市之间的火车票余量")
    public String checkTicket(String startStation, String endStation) {
        TicketQueryResult result = railwayTicketService.checkTicket(startStation, endStation);
        
        if (result.getStatus().equals("success")) {
            return String.format("查询成功！%s 到 %s 的火车票剩余 %d 张", 
                    startStation, endStation, result.getRemaining());
        } else {
            return String.format("查询失败：%s", result.getMessage());
        }
    }

    @Tool("预订火车票")
    public String bookTicket(String startStation, String endStation, int numSeats) {
        TicketBookingResult result = railwayTicketService.bookTicket(startStation, endStation, numSeats);
        
        if (result.getStatus().equals("success")) {
            return String.format("订票成功！已为您预订 %d 张从 %s 到 %s 的火车票", 
                    result.getBooked(), startStation, endStation);
        } else {
            return String.format("订票失败：%s", result.getMessage());
        }
    }
}
