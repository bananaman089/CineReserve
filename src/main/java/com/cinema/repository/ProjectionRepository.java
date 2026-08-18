package com.cinema.repository;

import com.cinema.model.Projection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectionRepository extends JpaRepository<Projection, Long> {

    List<Projection> findByMovieId(Long movieId);

    List<Projection> findByHallId(Long hallId);
}
