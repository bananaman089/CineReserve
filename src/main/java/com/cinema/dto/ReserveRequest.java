package com.cinema.dto;

import com.cinema.model.enums.TicketType;
import java.util.List;

public class ReserveRequest {
    private Long projectionId;
    private List<Long> seatIds;
    private TicketType ticketType = TicketType.STANDARD;

    public Long getProjectionId() {
        return projectionId;
    }

    public void setProjectionId(Long projectionId) {
        this.projectionId = projectionId;
    }

    public List<Long> getSeatIds() {
        return seatIds;
    }

    public void setSeatIds(List<Long> seatIds) {
        this.seatIds = seatIds;
    }

    public TicketType getTicketType() {
        return ticketType;
    }

    public void setTicketType(TicketType ticketType) {
        this.ticketType = ticketType;
    }
}
