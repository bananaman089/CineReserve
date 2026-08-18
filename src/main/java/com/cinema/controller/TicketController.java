package com.cinema.controller;

import com.cinema.dto.PayRequest;
import com.cinema.dto.ReserveRequest;
import com.cinema.model.Ticket;
import com.cinema.service.TicketService;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/projections/{projectionId}/occupied-seats")
    public Set<Long> getOccupiedSeats(@PathVariable Long projectionId) {
        return ticketService.getOccupiedSeatIds(projectionId);
    }

    @GetMapping("/projections/{projectionId}/tickets")
    public List<Ticket> getByProjection(@PathVariable Long projectionId) {
        return ticketService.findByProjectionId(projectionId);
    }

    @PostMapping("/tickets/reserve")
    public ResponseEntity<List<Ticket>> reserve(
            @RequestBody ReserveRequest request,
            Authentication authentication
    ) {
        List<Ticket> tickets = ticketService.reserveSeats(
                request.getProjectionId(),
                request.getSeatIds(),
                authentication.getName(),
                request.getTicketType()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(tickets);
    }

    @PostMapping("/tickets/pay")
    public List<Ticket> pay(@RequestBody PayRequest request, Authentication authentication) {
        return ticketService.payTickets(request.getTicketIds(), authentication.getName(), true);
    }

    @PostMapping("/cashier/sell")
    public ResponseEntity<List<Ticket>> sellAtCashier(
            @RequestBody ReserveRequest request,
            Authentication authentication
    ) {
        List<Ticket> tickets = ticketService.sellAtCashier(
                request.getProjectionId(),
                request.getSeatIds(),
                authentication.getName(),
                request.getTicketType()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(tickets);
    }

    @GetMapping("/tickets/my")
    public List<Ticket> myTickets(Authentication authentication) {
        return ticketService.findByUsername(authentication.getName());
    }
}
