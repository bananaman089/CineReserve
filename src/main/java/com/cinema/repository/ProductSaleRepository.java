package com.cinema.repository;

import com.cinema.model.ProductSale;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductSaleRepository extends JpaRepository<ProductSale, Long> {
    void deleteBySoldById(Long userId);

    List<ProductSale> findAllByOrderBySoldAtDesc();
}
