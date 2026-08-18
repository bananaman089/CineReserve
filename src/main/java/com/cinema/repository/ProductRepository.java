package com.cinema.repository;

import com.cinema.model.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrueOrderByCategoryAscNameAsc();

    List<Product> findAllByOrderByCategoryAscNameAsc();

    boolean existsByNameIgnoreCaseAndSizeLabelIgnoreCase(String name, String sizeLabel);
}
