package com.cinema.controller;

import com.cinema.dto.ProjectionRequest;
import com.cinema.model.Projection;
import com.cinema.service.ProjectionService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ProjectionController {

    private final ProjectionService projectionService;

    public ProjectionController(ProjectionService projectionService) {
        this.projectionService = projectionService;
    }

    @GetMapping("/projections")
    public List<Projection> getAll() {
        return projectionService.findAll();
    }

    @GetMapping("/projections/{id}")
    public Projection getById(@PathVariable Long id) {
        return projectionService.findById(id);
    }

    @GetMapping("/movies/{movieId}/projections")
    public List<Projection> getByMovie(@PathVariable Long movieId) {
        return projectionService.findByMovieId(movieId);
    }

    @PostMapping("/admin/projections")
    public ResponseEntity<?> create(@RequestBody ProjectionRequest request) {
        List<Long> hallIds = request.getHallIds();
        if (hallIds != null && !hallIds.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(projectionService.createMany(
                    request.getMovieId(),
                    hallIds,
                    request.getStartTime(),
                    request.getBasePrice(),
                    request.getFormat()
            ));
        }
        Projection projection = projectionService.create(
                request.getMovieId(),
                request.getHallId(),
                request.getStartTime(),
                request.getBasePrice(),
                request.getFormat()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(projection);
    }

    @DeleteMapping("/admin/projections/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        projectionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
