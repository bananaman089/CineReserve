package com.cinema.service;

import com.cinema.exception.BusinessException;
import com.cinema.model.Cinema;
import com.cinema.repository.CinemaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CinemaService {

    private final CinemaRepository cinemaRepository;

    public CinemaService(CinemaRepository cinemaRepository) {
        this.cinemaRepository = cinemaRepository;
    }

    public List<Cinema> findAll() {
        return cinemaRepository.findAll();
    }

    public Cinema findById(Long id) {
        return cinemaRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Киното не е намерено"));
    }

    public List<Cinema> findByCity(String city) {
        return cinemaRepository.findByCityIgnoreCase(city);
    }

    @Transactional
    public Cinema create(Cinema cinema) {
        cinema.setCity(resolveCity(cinema.getCity()));
        return cinemaRepository.save(cinema);
    }

    @Transactional
    public Cinema update(Long id, Cinema updated) {
        Cinema cinema = findById(id);
        cinema.setName(updated.getName());
        cinema.setCity(resolveCity(updated.getCity()));
        cinema.setAddress(updated.getAddress());
        return cinemaRepository.save(cinema);
    }

    @Transactional
    public void delete(Long id) {
        if (!cinemaRepository.existsById(id)) {
            throw new BusinessException("Киното не е намерено");
        }
        cinemaRepository.deleteById(id);
    }

    private String resolveCity(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException("Градът е задължителен");
        }
        String city = raw.trim().replaceAll("\\s+", " ");
        return cinemaRepository.findByCityIgnoreCase(city).stream()
                .map(Cinema::getCity)
                .findFirst()
                .orElse(city);
    }
}
