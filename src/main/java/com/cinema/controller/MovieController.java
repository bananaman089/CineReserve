package com.cinema.controller;

import com.cinema.dto.MovieRequest;
import com.cinema.model.Movie;
import com.cinema.service.MovieService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping("/movies")
    public List<Movie> getAll() {
        return movieService.findAll();
    }

    @GetMapping("/movies/{id}")
    public Movie getById(@PathVariable Long id) {
        return movieService.findById(id);
    }

    @PostMapping("/admin/movies")
    public ResponseEntity<Movie> create(@RequestBody MovieRequest request) {
        Movie movie = new Movie();
        movie.setTitle(request.getTitle());
        movie.setDescription(request.getDescription());
        movie.setDuration(request.getDuration());
        return ResponseEntity.status(HttpStatus.CREATED).body(movieService.create(movie));
    }

    @PutMapping("/admin/movies/{id}")
    public Movie update(@PathVariable Long id, @RequestBody MovieRequest request) {
        Movie movie = new Movie();
        movie.setTitle(request.getTitle());
        movie.setDescription(request.getDescription());
        movie.setDuration(request.getDuration());
        return movieService.update(id, movie);
    }

    @DeleteMapping("/admin/movies/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        movieService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
