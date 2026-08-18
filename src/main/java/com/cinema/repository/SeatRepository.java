package com.cinema.repository;

import com.cinema.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByHallIdOrderByRowNumAscSeatNumAsc(Long hallId);

    void deleteByHallId(Long hallId);
}
