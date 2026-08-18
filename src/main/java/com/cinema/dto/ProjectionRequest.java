package com.cinema.dto;

import com.cinema.model.enums.ProjectionFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProjectionRequest {
    private Long movieId;
    private Long hallId;
    private LocalDateTime startTime;
    private BigDecimal basePrice;
    private ProjectionFormat format = ProjectionFormat.TWO_D;
    private java.util.List<Long> hallIds;

    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
    }

    public Long getHallId() {
        return hallId;
    }

    public void setHallId(Long hallId) {
        this.hallId = hallId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public ProjectionFormat getFormat() {
        return format;
    }

    public void setFormat(ProjectionFormat format) {
        this.format = format;
    }

    public java.util.List<Long> getHallIds() {
        return hallIds;
    }

    public void setHallIds(java.util.List<Long> hallIds) {
        this.hallIds = hallIds;
    }
}
