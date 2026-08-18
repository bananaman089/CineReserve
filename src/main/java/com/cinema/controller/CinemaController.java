package com.cinema.controller;

import com.cinema.dto.CinemaRequest;
import com.cinema.model.Cinema;
import com.cinema.service.CinemaService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CinemaController {

    private final CinemaService cinemaService;

    public CinemaController(CinemaService cinemaService) {
        this.cinemaService = cinemaService;
    }

    @GetMapping("/cinemas")
    public List<Cinema> getAll() {
        return cinemaService.findAll();
    }

    @GetMapping("/cinemas/{id}")
    public Cinema getById(@PathVariable Long id) {
        return cinemaService.findById(id);
    }

    @PostMapping("/admin/cinemas")
    public ResponseEntity<Cinema> create(@RequestBody CinemaRequest request) {
        Cinema cinema = new Cinema();
        cinema.setName(request.getName());
        cinema.setCity(request.getCity());
        cinema.setAddress(request.getAddress());
        return ResponseEntity.status(HttpStatus.CREATED).body(cinemaService.create(cinema));
    }

    @PutMapping("/admin/cinemas/{id}")
    public Cinema update(@PathVariable Long id, @RequestBody CinemaRequest request) {
        Cinema cinema = new Cinema();
        cinema.setName(request.getName());
        cinema.setCity(request.getCity());
        cinema.setAddress(request.getAddress());
        return cinemaService.update(id, cinema);
    }

    @DeleteMapping("/admin/cinemas/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cinemaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
