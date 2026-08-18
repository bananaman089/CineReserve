package com.cinema.controller;

import com.cinema.service.ReportService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/revenue-by-movie")
    public List<Map<String, Object>> revenueByMovie(@RequestParam Long cinemaId) {
        return reportService.revenueByMovie(cinemaId);
    }

    @GetMapping("/occupancy/{projectionId}")
    public Map<String, Object> occupancy(@PathVariable Long projectionId) {
        return reportService.hallOccupancy(projectionId);
    }

    @GetMapping("/product-sales")
    public Map<String, Object> productSales(@RequestParam Long cinemaId) {
        return reportService.productSales(cinemaId);
    }
}
