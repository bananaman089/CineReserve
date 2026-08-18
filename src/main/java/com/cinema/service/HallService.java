package com.cinema.service;

import com.cinema.exception.BusinessException;
import com.cinema.model.Cinema;
import com.cinema.model.Hall;
import com.cinema.model.Seat;
import com.cinema.repository.HallRepository;
import com.cinema.repository.SeatRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HallService {

    private final HallRepository hallRepository;
    private final SeatRepository seatRepository;
    private final CinemaService cinemaService;

    public HallService(
            HallRepository hallRepository,
            SeatRepository seatRepository,
            CinemaService cinemaService
    ) {
        this.hallRepository = hallRepository;
        this.seatRepository = seatRepository;
        this.cinemaService = cinemaService;
    }

    public List<Hall> findAll() {
        return hallRepository.findAll();
    }

    public Hall findById(Long id) {
        return hallRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Залата не е намерена"));
    }

    public List<Hall> findByCinemaId(Long cinemaId) {
        cinemaService.findById(cinemaId);
        return hallRepository.findByCinemaId(cinemaId);
    }

    public List<Seat> findSeatsByHallId(Long hallId) {
        findById(hallId);
        return seatRepository.findByHallIdOrderByRowNumAscSeatNumAsc(hallId);
    }

    /**
     * При създаване на зала автоматично генерираме всички места.
     * Пример: 10 реда x 10 места = 100 записа в таблица seats.
     */
    @Transactional
    public Hall create(Long cinemaId, String name, Integer rowsCount, Integer seatsPerRow) {
        if (rowsCount == null || rowsCount <= 0 || seatsPerRow == null || seatsPerRow <= 0) {
            throw new BusinessException("Редовете и местата на ред трябва да са положителни числа");
        }

        Cinema cinema = cinemaService.findById(cinemaId);

        Hall hall = new Hall();
        hall.setName(name);
        hall.setCinema(cinema);
        hall.setRowsCount(rowsCount);
        hall.setSeatsPerRow(seatsPerRow);
        Hall savedHall = hallRepository.save(hall);

        List<Seat> seats = new ArrayList<>();
        for (int row = 1; row <= rowsCount; row++) {
            for (int seatNum = 1; seatNum <= seatsPerRow; seatNum++) {
                Seat seat = new Seat();
                seat.setHall(savedHall);
                seat.setRowNum(row);
                seat.setSeatNum(seatNum);
                seats.add(seat);
            }
        }
        seatRepository.saveAll(seats);

        return savedHall;
    }

    @Transactional
    public Hall update(Long id, String name) {
        Hall hall = findById(id);
        hall.setName(name);
        return hallRepository.save(hall);
    }

    @Transactional
    public void delete(Long id) {
        Hall hall = findById(id);
        seatRepository.deleteByHallId(hall.getId());
        hallRepository.delete(hall);
    }
}
