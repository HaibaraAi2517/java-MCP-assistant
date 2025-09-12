package com.ygk.bean;

/**
 * 火车票预订结果
 */
public class TicketBookingResult {
    private String status;
    private String message;
    private Integer booked;

    public TicketBookingResult() {}

    public TicketBookingResult(boolean success, String message, Integer booked) {
        this.status = success ? "success" : "fail";
        this.message = message;
        this.booked = booked;
    }

    public TicketBookingResult(String status, String message, Integer booked) {
        this.status = status;
        this.message = message;
        this.booked = booked;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getBooked() {
        return booked;
    }

    public void setBooked(Integer booked) {
        this.booked = booked;
    }

    @Override
    public String toString() {
        return "TicketBookingResult{" +
                "status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", booked=" + booked +
                '}';
    }
}
