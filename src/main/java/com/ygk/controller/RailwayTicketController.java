package com.ygk.controller;

import com.ygk.bean.TicketBookingResult;
import com.ygk.bean.TicketQueryResult;
import com.ygk.service.RailwayTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 火车票控制器
 * 提供火车票查询和预订的 REST API
 */
@Tag(name = "火车票服务")
@RestController
@RequestMapping("/railway")
public class RailwayTicketController {

    @Autowired
    private RailwayTicketService railwayTicketService;

    @Operation(summary = "查询火车票余量")
    @GetMapping("/check")
    public TicketQueryResult checkTicket(
            @RequestParam("startStation") String startStation,
            @RequestParam("endStation") String endStation) {
        return railwayTicketService.checkTicket(startStation, endStation);
    }

    @Operation(summary = "预订火车票")
    @PostMapping("/book")
    public TicketBookingResult bookTicket(
            @RequestParam("startStation") String startStation,
            @RequestParam("endStation") String endStation,
            @RequestParam("numSeats") int numSeats) {
        return railwayTicketService.bookTicket(startStation, endStation, numSeats);
    }
}
