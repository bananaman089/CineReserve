package com.cinema.repository;

import com.cinema.model.Cinema;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CinemaRepository extends JpaRepository<Cinema, Long> {

    List<Cinema> findByCityIgnoreCase(String city);
}
