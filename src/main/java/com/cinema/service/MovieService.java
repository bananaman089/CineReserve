package com.cinema.service;

import com.cinema.exception.BusinessException;
import com.cinema.model.Movie;
import com.cinema.repository.MovieRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovieService {

    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public List<Movie> findAll() {
        return movieRepository.findAll();
    }

    public Movie findById(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Филмът не е намерен"));
    }

    @Transactional
    public Movie create(Movie movie) {
        if (movie.getDuration() == null || movie.getDuration() <= 0) {
            throw new BusinessException("Продължителността трябва да е положително число");
        }
        return movieRepository.save(movie);
    }

    @Transactional
    public Movie update(Long id, Movie updated) {
        Movie movie = findById(id);
        movie.setTitle(updated.getTitle());
        movie.setDescription(updated.getDescription());
        movie.setDuration(updated.getDuration());
        return movieRepository.save(movie);
    }

    @Transactional
    public void delete(Long id) {
        if (!movieRepository.existsById(id)) {
            throw new BusinessException("Филмът не е намерен");
        }
        movieRepository.deleteById(id);
    }
}
