package com.cinema.controller;

import com.cinema.dto.HallRequest;
import com.cinema.model.Hall;
import com.cinema.model.Seat;
import com.cinema.service.HallService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class HallController {

    private final HallService hallService;

    public HallController(HallService hallService) {
        this.hallService = hallService;
    }

    @GetMapping("/halls")
    public List<Hall> getAll() {
        return hallService.findAll();
    }

    @GetMapping("/halls/{id}")
    public Hall getById(@PathVariable Long id) {
        return hallService.findById(id);
    }

    @GetMapping("/cinemas/{cinemaId}/halls")
    public List<Hall> getByCinema(@PathVariable Long cinemaId) {
        return hallService.findByCinemaId(cinemaId);
    }

    @GetMapping("/halls/{id}/seats")
    public List<Seat> getSeats(@PathVariable Long id) {
        return hallService.findSeatsByHallId(id);
    }

    @PostMapping("/admin/halls")
    public ResponseEntity<Hall> create(@RequestBody HallRequest request) {
        Hall hall = hallService.create(
                request.getCinemaId(),
                request.getName(),
                request.getRowsCount(),
                request.getSeatsPerRow()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(hall);
    }

    @DeleteMapping("/admin/halls/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        hallService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
