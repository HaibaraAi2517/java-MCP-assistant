package com.ygk.bean;

/**
 * 火车票查询结果
 */
public class TicketQueryResult {
    private String status;
    private String message;
    private Integer remaining;

    public TicketQueryResult() {}

    public TicketQueryResult(boolean success, String message, Integer remaining) {
        this.status = success ? "success" : "fail";
        this.message = message;
        this.remaining = remaining;
    }

    public TicketQueryResult(String status, String message, Integer remaining) {
        this.status = status;
        this.message = message;
        this.remaining = remaining;
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

    public Integer getRemaining() {
        return remaining;
    }

    public void setRemaining(Integer remaining) {
        this.remaining = remaining;
    }

    @Override
    public String toString() {
        return "TicketQueryResult{" +
                "status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", remaining=" + remaining +
                '}';
    }
}
