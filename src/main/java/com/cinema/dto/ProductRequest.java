package com.cinema.dto;

import com.cinema.model.enums.ProductCategory;
import java.math.BigDecimal;

public class ProductRequest {
    private String name;
    private ProductCategory category;
    private String sizeLabel;
    private BigDecimal price;
    private Boolean active;
    private Boolean allowsSauce;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public String getSizeLabel() {
        return sizeLabel;
    }

    public void setSizeLabel(String sizeLabel) {
        this.sizeLabel = sizeLabel;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getAllowsSauce() {
        return allowsSauce;
    }

    public void setAllowsSauce(Boolean allowsSauce) {
        this.allowsSauce = allowsSauce;
    }
}
