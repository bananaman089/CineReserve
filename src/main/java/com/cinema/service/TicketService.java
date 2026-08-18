package com.cinema.service;

import com.cinema.exception.BusinessException;
import com.cinema.model.Projection;
import com.cinema.model.Seat;
import com.cinema.model.Ticket;
import com.cinema.model.User;
import com.cinema.model.enums.TicketStatus;
import com.cinema.model.enums.TicketType;
import com.cinema.repository.SeatRepository;
import com.cinema.repository.TicketRepository;
import com.cinema.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private static final int RESERVATION_MINUTES = 5;
    private static final BigDecimal STUDENT_DISCOUNT = new BigDecimal("0.80");

    private final TicketRepository ticketRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final ProjectionService projectionService;
    private final AccountService accountService;

    public TicketService(
            TicketRepository ticketRepository,
            SeatRepository seatRepository,
            UserRepository userRepository,
            ProjectionService projectionService,
            AccountService accountService
    ) {
        this.ticketRepository = ticketRepository;
        this.seatRepository = seatRepository;
        this.userRepository = userRepository;
        this.projectionService = projectionService;
        this.accountService = accountService;
    }

    public List<Ticket> findByProjectionId(Long projectionId) {
        projectionService.findById(projectionId);
        cleanupExpiredPending(projectionId);
        return ticketRepository.findByProjectionId(projectionId);
    }

    public List<Ticket> findByUserId(Long userId) {
        return ticketRepository.findByUserId(userId);
    }

    public List<Ticket> findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Потребителят не е намерен"));
        return ticketRepository.findByUserId(user.getId()).stream()
                .filter(t -> t.getStatus() == TicketStatus.PAID)
                .toList();
    }

    public Ticket findById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Билетът не е намерен"));
    }

    /**
     * Връща ID-тата на заетите места за дадена прожекция.
     * Заети = PAID или PENDING, което още не е изтекло.
     */
    public Set<Long> getOccupiedSeatIds(Long projectionId) {
        cleanupExpiredPending(projectionId);

        List<TicketStatus> activeStatuses = List.of(TicketStatus.PAID, TicketStatus.PENDING);
        List<Ticket> activeTickets = ticketRepository.findByProjectionIdAndStatusIn(projectionId, activeStatuses);

        Set<Long> occupied = new HashSet<>();
        LocalDateTime now = LocalDateTime.now();

        for (Ticket ticket : activeTickets) {
            if (ticket.getStatus() == TicketStatus.PAID) {
                occupied.add(ticket.getSeat().getId());
            } else if (ticket.getStatus() == TicketStatus.PENDING
                    && ticket.getReservedUntil() != null
                    && ticket.getReservedUntil().isAfter(now)) {
                occupied.add(ticket.getSeat().getId());
            }
        }
        return occupied;
    }

    /**
     * Клиентът избира места -> статус PENDING за 5 минути.
     * При конфликт (Optimistic Lock / вече заето) връщаме приятелско съобщение.
     */
    @Transactional
    public List<Ticket> reserveSeats(
            Long projectionId,
            List<Long> seatIds,
            String username,
            TicketType ticketType
    ) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new BusinessException("Избери поне едно място");
        }

        Projection projection = projectionService.findById(projectionId);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Потребителят не е намерен"));

        cleanupExpiredPending(projectionId);
        Set<Long> occupied = getOccupiedSeatIds(projectionId);

        BigDecimal price = calculatePrice(projection.getBasePrice(), ticketType);
        LocalDateTime reservedUntil = LocalDateTime.now().plusMinutes(RESERVATION_MINUTES);
        List<Ticket> created = new ArrayList<>();

        try {
            for (Long seatId : seatIds) {
                if (occupied.contains(seatId)) {
                    throw new BusinessException("Мястото току-що беше резервирано от друг");
                }

                Seat seat = seatRepository.findById(seatId)
                        .orElseThrow(() -> new BusinessException("Мястото не е намерено"));

                if (!seat.getHall().getId().equals(projection.getHall().getId())) {
                    throw new BusinessException("Мястото не принадлежи на залата на тази прожекция");
                }

                // Ако има стар CANCELLED билет за същото място, обновяваме него
                Ticket ticket = ticketRepository
                        .findByProjectionIdAndSeatId(projectionId, seatId)
                        .orElse(new Ticket());

                if (ticket.getId() != null
                        && ticket.getStatus() != TicketStatus.CANCELLED
                        && !(ticket.getStatus() == TicketStatus.PENDING
                        && ticket.getReservedUntil() != null
                        && ticket.getReservedUntil().isBefore(LocalDateTime.now()))) {
                    throw new BusinessException("Мястото току-що беше резервирано от друг");
                }

                ticket.setProjection(projection);
                ticket.setSeat(seat);
                ticket.setUser(user);
                ticket.setStatus(TicketStatus.PENDING);
                ticket.setPrice(price);
                ticket.setUniqueCode(null);
                ticket.setReservedUntil(reservedUntil);

                created.add(ticketRepository.save(ticket));
            }
            return created;
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new BusinessException("Мястото току-що беше резервирано от друг");
        }
    }

    /**
     * Плащане: PENDING -> PAID + четим код (ред / място / цена).
     * chargeBalance=true за клиент (тегли от портфейла).
     * chargeBalance=false за каса (плащане в брой).
     */
    @Transactional
    public List<Ticket> payTickets(List<Long> ticketIds, String username, boolean chargeBalance) {
        if (ticketIds == null || ticketIds.isEmpty()) {
            throw new BusinessException("Няма билети за плащане");
        }

        List<Ticket> toPay = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Long ticketId : ticketIds) {
            Ticket ticket = findById(ticketId);

            if (!ticket.getUser().getUsername().equals(username)) {
                throw new BusinessException("Този билет не принадлежи на текущия потребител");
            }
            if (ticket.getStatus() != TicketStatus.PENDING) {
                throw new BusinessException("Билетът не е в статус PENDING");
            }
            if (ticket.getReservedUntil() != null && ticket.getReservedUntil().isBefore(LocalDateTime.now())) {
                ticket.setStatus(TicketStatus.CANCELLED);
                ticketRepository.save(ticket);
                throw new BusinessException("Времето за плащане изтече. Резервирай местата отново.");
            }

            toPay.add(ticket);
            total = total.add(ticket.getPrice());
        }

        if (chargeBalance) {
            accountService.charge(username, total);
        }

        List<Ticket> paid = new ArrayList<>();
        try {
            for (Ticket ticket : toPay) {
                ticket.setStatus(TicketStatus.PAID);
                ticket.setUniqueCode(buildReadableCode(ticket));
                ticket.setReservedUntil(null);
                paid.add(ticketRepository.save(ticket));
            }
            return paid;
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new BusinessException("Мястото току-що беше резервирано от друг");
        }
    }

    private String buildReadableCode(Ticket ticket) {
        int row = ticket.getSeat().getRowNum();
        int seat = ticket.getSeat().getSeatNum();
        String price = ticket.getPrice().setScale(2, RoundingMode.HALF_UP).toPlainString();
        // ticket id прави кода уникален, ако мястото се продаде отново по-късно
        return "Ред " + row + " / място " + seat + " · " + price + " € (#" + ticket.getId() + ")";
    }

    /**
     * Касиер: директно маркира места като платени в брой (без баланс).
     */
    @Transactional
    public List<Ticket> sellAtCashier(
            Long projectionId,
            List<Long> seatIds,
            String cashierUsername,
            TicketType ticketType
    ) {
        List<Ticket> reserved = reserveSeats(projectionId, seatIds, cashierUsername, ticketType);
        List<Long> ids = reserved.stream().map(Ticket::getId).toList();
        return payTickets(ids, cashierUsername, false);
    }

    public BigDecimal calculatePrice(BigDecimal basePrice, TicketType ticketType) {
        TicketType type = ticketType == null ? TicketType.STANDARD : ticketType;
        if (type == TicketType.STUDENT) {
            return basePrice.multiply(STUDENT_DISCOUNT).setScale(2, RoundingMode.HALF_UP);
        }
        return basePrice.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public void cleanupExpiredPending(Long projectionId) {
        List<Ticket> expired = ticketRepository.findByStatusAndReservedUntilBefore(
                TicketStatus.PENDING,
                LocalDateTime.now()
        );

        for (Ticket ticket : expired) {
            if (projectionId == null || ticket.getProjection().getId().equals(projectionId)) {
                ticket.setStatus(TicketStatus.CANCELLED);
                ticketRepository.save(ticket);
            }
        }
    }
}
