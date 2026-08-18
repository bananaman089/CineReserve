package com.cinema.repository;

import com.cinema.model.Ticket;
import com.cinema.model.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByProjectionId(Long projectionId);

    List<Ticket> findByUserId(Long userId);

    Optional<Ticket> findByProjectionIdAndSeatId(Long projectionId, Long seatId);

    List<Ticket> findByProjectionIdAndStatusIn(Long projectionId, List<TicketStatus> statuses);

    List<Ticket> findByStatusAndReservedUntilBefore(TicketStatus status, LocalDateTime moment);

    void deleteByUserId(Long userId);
}
