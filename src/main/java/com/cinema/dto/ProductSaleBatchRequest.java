package com.cinema.dto;

import java.util.ArrayList;
import java.util.List;

public class ProductSaleBatchRequest {
    private List<ProductSaleRequest> items = new ArrayList<>();
    private String paymentMethod;
    private Long cinemaId;

    public List<ProductSaleRequest> getItems() {
        return items;
    }

    public void setItems(List<ProductSaleRequest> items) {
        this.items = items;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Long getCinemaId() {
        return cinemaId;
    }

    public void setCinemaId(Long cinemaId) {
        this.cinemaId = cinemaId;
    }
}
