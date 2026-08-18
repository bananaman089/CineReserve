package com.cinema.service;

import com.cinema.model.ProductSale;
import com.cinema.model.Ticket;
import com.cinema.model.enums.TicketStatus;
import com.cinema.repository.ProductSaleRepository;
import com.cinema.repository.TicketRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private final TicketRepository ticketRepository;
    private final ProductSaleRepository productSaleRepository;
    private final HallService hallService;
    private final TicketService ticketService;
    private final ProjectionService projectionService;

    public ReportService(
            TicketRepository ticketRepository,
            ProductSaleRepository productSaleRepository,
            HallService hallService,
            TicketService ticketService,
            ProjectionService projectionService
    ) {
        this.ticketRepository = ticketRepository;
        this.productSaleRepository = productSaleRepository;
        this.hallService = hallService;
        this.ticketService = ticketService;
        this.projectionService = projectionService;
    }

    public List<Map<String, Object>> revenueByMovie(Long cinemaId) {
        Map<String, BigDecimal> totals = new HashMap<>();
        Map<String, Integer> ticketCounts = new HashMap<>();

        List<Ticket> paidTickets = ticketRepository.findAll().stream()
                .filter(t -> t.getStatus() == TicketStatus.PAID)
                .filter(t -> t.getProjection() != null
                        && t.getProjection().getHall() != null
                        && t.getProjection().getHall().getCinema() != null
                        && cinemaId.equals(t.getProjection().getHall().getCinema().getId()))
                .toList();

        for (Ticket ticket : paidTickets) {
            String title = ticket.getProjection().getMovie().getTitle();
            totals.merge(title, ticket.getPrice(), BigDecimal::add);
            ticketCounts.merge(title, 1, Integer::sum);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : totals.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("movieTitle", entry.getKey());
            row.put("tickets", ticketCounts.getOrDefault(entry.getKey(), 0));
            row.put("revenue", entry.getValue());
            result.add(row);
        }
        return result;
    }

    /**
     * Заетост за конкретна прожекция:
     * - платени места
     * - временно заключени (PENDING)
     * - свободни
     * - % върху платените (реална продажба)
     */
    public Map<String, Object> hallOccupancy(Long projectionId) {
        var projection = projectionService.findById(projectionId);
        Long hallId = projection.getHall().getId();

        int totalSeats = hallService.findSeatsByHallId(hallId).size();

        // изчистваме изтекли PENDING преди броене
        ticketService.cleanupExpiredPending(projectionId);

        List<Ticket> tickets = ticketRepository.findByProjectionId(projectionId);
        Set<Long> paidSeatIds = new HashSet<>();
        Set<Long> pendingSeatIds = new HashSet<>();

        for (Ticket ticket : tickets) {
            if (ticket.getSeat() == null) {
                continue;
            }
            Long seatId = ticket.getSeat().getId();
            if (ticket.getStatus() == TicketStatus.PAID) {
                paidSeatIds.add(seatId);
            } else if (ticket.getStatus() == TicketStatus.PENDING) {
                pendingSeatIds.add(seatId);
            }
        }

        // ако мястото е платено, не го броим и като pending
        pendingSeatIds.removeAll(paidSeatIds);

        int paidSeats = paidSeatIds.size();
        int pendingSeats = pendingSeatIds.size();
        int occupiedSeats = paidSeats + pendingSeats;
        int freeSeats = Math.max(0, totalSeats - occupiedSeats);

        BigDecimal soldPercent = BigDecimal.ZERO;
        BigDecimal occupiedPercent = BigDecimal.ZERO;
        if (totalSeats > 0) {
            soldPercent = BigDecimal.valueOf(paidSeats)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalSeats), 2, RoundingMode.HALF_UP);
            occupiedPercent = BigDecimal.valueOf(occupiedSeats)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalSeats), 2, RoundingMode.HALF_UP);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("projectionId", projectionId);
        result.put("hallName", projection.getHall().getName());
        result.put("movieTitle", projection.getMovie().getTitle());
        result.put("startTime", projection.getStartTime().toString());
        result.put("totalSeats", totalSeats);
        result.put("paidSeats", paidSeats);
        result.put("pendingSeats", pendingSeats);
        result.put("occupiedSeats", occupiedSeats);
        result.put("freeSeats", freeSeats);
        result.put("soldPercent", soldPercent);
        result.put("occupancyPercent", occupiedPercent);
        return result;
    }

    public Map<String, Object> productSales(Long cinemaId) {
        List<ProductSale> sales = productSaleRepository.findAllByOrderBySoldAtDesc().stream()
                .filter(sale -> sale.getCinema() != null && cinemaId.equals(sale.getCinema().getId()))
                .toList();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        int totalQuantity = 0;
        Map<String, Map<String, Object>> byProduct = new LinkedHashMap<>();
        Map<String, Map<String, Object>> byCategory = new LinkedHashMap<>();
        Map<String, Map<String, Object>> byCashier = new LinkedHashMap<>();
        List<Map<String, Object>> details = new ArrayList<>();

        for (ProductSale sale : sales) {
            BigDecimal lineTotal = sale.getTotalPrice() == null ? BigDecimal.ZERO : sale.getTotalPrice();
            int qty = sale.getQuantity() == null ? 0 : sale.getQuantity();
            totalRevenue = totalRevenue.add(lineTotal);
            totalQuantity += qty;

            String productName = sale.getProduct() != null ? sale.getProduct().getName() : "—";
            String sizeLabel = sale.getProduct() != null ? sale.getProduct().getSizeLabel() : null;
            String category = sale.getProduct() != null && sale.getProduct().getCategory() != null
                    ? sale.getProduct().getCategory().name()
                    : "OTHER";
            String cashier = sale.getSoldBy() != null ? sale.getSoldBy().getUsername() : "—";
            String productKey = productName + "|" + (sizeLabel == null ? "" : sizeLabel) + "|" + category;

            Map<String, Object> productRow = byProduct.computeIfAbsent(productKey, key -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("productName", productName);
                row.put("sizeLabel", sizeLabel);
                row.put("category", category);
                row.put("quantity", 0);
                row.put("revenue", BigDecimal.ZERO);
                return row;
            });
            productRow.put("quantity", (int) productRow.get("quantity") + qty);
            productRow.put("revenue", ((BigDecimal) productRow.get("revenue")).add(lineTotal));

            Map<String, Object> categoryRow = byCategory.computeIfAbsent(category, key -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("category", category);
                row.put("quantity", 0);
                row.put("revenue", BigDecimal.ZERO);
                return row;
            });
            categoryRow.put("quantity", (int) categoryRow.get("quantity") + qty);
            categoryRow.put("revenue", ((BigDecimal) categoryRow.get("revenue")).add(lineTotal));

            Map<String, Object> cashierRow = byCashier.computeIfAbsent(cashier, key -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("cashier", cashier);
                row.put("orders", 0);
                row.put("quantity", 0);
                row.put("revenue", BigDecimal.ZERO);
                return row;
            });
            cashierRow.put("orders", (int) cashierRow.get("orders") + 1);
            cashierRow.put("quantity", (int) cashierRow.get("quantity") + qty);
            cashierRow.put("revenue", ((BigDecimal) cashierRow.get("revenue")).add(lineTotal));

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("soldAt", sale.getSoldAt() != null ? sale.getSoldAt().toString() : "");
            detail.put("productName", productName);
            detail.put("sizeLabel", sizeLabel);
            detail.put("category", category);
            detail.put("sauce", sale.getSauce());
            detail.put("quantity", qty);
            detail.put("unitPrice", sale.getUnitPrice());
            detail.put("totalPrice", lineTotal);
            detail.put("cashier", cashier);
            details.add(detail);
        }

        List<Map<String, Object>> productRows = new ArrayList<>(byProduct.values());
        productRows.sort(Comparator.comparing((Map<String, Object> row) -> (BigDecimal) row.get("revenue")).reversed());
        List<Map<String, Object>> categoryRows = new ArrayList<>(byCategory.values());
        categoryRows.sort(Comparator.comparing((Map<String, Object> row) -> (BigDecimal) row.get("revenue")).reversed());
        List<Map<String, Object>> cashierRows = new ArrayList<>(byCashier.values());
        cashierRows.sort(Comparator.comparing((Map<String, Object> row) -> (BigDecimal) row.get("revenue")).reversed());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalRevenue", totalRevenue);
        summary.put("totalQuantity", totalQuantity);
        summary.put("totalOrders", sales.size());
        summary.put("uniqueProducts", productRows.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("byProduct", productRows);
        result.put("byCategory", categoryRows);
        result.put("byCashier", cashierRows);
        result.put("details", details);
        return result;
    }
}
