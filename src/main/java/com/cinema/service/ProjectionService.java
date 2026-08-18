package com.cinema.service;

import com.cinema.exception.BusinessException;
import com.cinema.model.Hall;
import com.cinema.model.Movie;
import com.cinema.model.Projection;
import com.cinema.model.enums.ProjectionFormat;
import com.cinema.repository.ProjectionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectionService {

    private final ProjectionRepository projectionRepository;
    private final MovieService movieService;
    private final HallService hallService;

    public ProjectionService(
            ProjectionRepository projectionRepository,
            MovieService movieService,
            HallService hallService
    ) {
        this.projectionRepository = projectionRepository;
        this.movieService = movieService;
        this.hallService = hallService;
    }

    public List<Projection> findAll() {
        return projectionRepository.findAll().stream()
                .sorted(Comparator.comparing(Projection::getStartTime))
                .toList();
    }

    public Projection findById(Long id) {
        return projectionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Прожекцията не е намерена"));
    }

    public List<Projection> findByMovieId(Long movieId) {
        movieService.findById(movieId);
        return projectionRepository.findByMovieId(movieId).stream()
                .sorted(Comparator.comparing(Projection::getStartTime))
                .toList();
    }

    public List<Projection> findByHallId(Long hallId) {
        hallService.findById(hallId);
        return projectionRepository.findByHallId(hallId).stream()
                .sorted(Comparator.comparing(Projection::getStartTime))
                .toList();
    }

    @Transactional
    public Projection create(
            Long movieId,
            Long hallId,
            LocalDateTime startTime,
            BigDecimal basePrice,
            ProjectionFormat format
    ) {
        if (startTime == null || startTime.isBefore(LocalDateTime.now())) {
            throw new BusinessException("Началният час трябва да е в бъдещето");
        }
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Цената трябва да е положително число");
        }

        Movie movie = movieService.findById(movieId);
        Hall hall = hallService.findById(hallId);

        Projection projection = new Projection();
        projection.setMovie(movie);
        projection.setHall(hall);
        projection.setStartTime(startTime);
        projection.setBasePrice(basePrice);
        projection.setFormat(format == null ? ProjectionFormat.TWO_D : format);
        return projectionRepository.save(projection);
    }

    @Transactional
    public List<Projection> createMany(
            Long movieId,
            List<Long> hallIds,
            LocalDateTime startTime,
            BigDecimal basePrice,
            ProjectionFormat format
    ) {
        if (hallIds == null || hallIds.isEmpty()) {
            throw new BusinessException("Избери поне една зала");
        }
        List<Projection> created = new java.util.ArrayList<>();
        for (Long hallId : hallIds) {
            created.add(create(movieId, hallId, startTime, basePrice, format));
        }
        return created;
    }

    @Transactional
    public Projection update(Long id, LocalDateTime startTime, BigDecimal basePrice, ProjectionFormat format) {
        Projection projection = findById(id);
        if (startTime != null) {
            projection.setStartTime(startTime);
        }
        if (basePrice != null) {
            if (basePrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Цената трябва да е положително число");
            }
            projection.setBasePrice(basePrice);
        }
        if (format != null) {
            projection.setFormat(format);
        }
        return projectionRepository.save(projection);
    }

    @Transactional
    public void delete(Long id) {
        if (!projectionRepository.existsById(id)) {
            throw new BusinessException("Прожекцията не е намерена");
        }
        projectionRepository.deleteById(id);
    }
}
