package com.cinema.service;

import com.cinema.dto.ProductSaleRequest;
import com.cinema.exception.BusinessException;
import com.cinema.model.Cinema;
import com.cinema.model.Product;
import com.cinema.model.ProductSale;
import com.cinema.model.User;
import com.cinema.model.enums.ProductCategory;
import com.cinema.repository.ProductRepository;
import com.cinema.repository.ProductSaleRepository;
import com.cinema.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private static final Set<String> SAUCES = Set.of("CHEDDAR", "BBQ");

    private final ProductRepository productRepository;
    private final ProductSaleRepository productSaleRepository;
    private final UserRepository userRepository;
    private final CinemaService cinemaService;

    public ProductService(
            ProductRepository productRepository,
            ProductSaleRepository productSaleRepository,
            UserRepository userRepository,
            CinemaService cinemaService
    ) {
        this.productRepository = productRepository;
        this.productSaleRepository = productSaleRepository;
        this.userRepository = userRepository;
        this.cinemaService = cinemaService;
    }

    public List<Product> findActive() {
        return productRepository.findByActiveTrueOrderByCategoryAscNameAsc();
    }

    public List<Product> findAll() {
        return productRepository.findAllByOrderByCategoryAscNameAsc();
    }

    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Продуктът не е намерен"));
    }

    @Transactional
    public Product create(Product product) {
        validate(product);
        product.setActive(true);
        return productRepository.save(product);
    }

    @Transactional
    public Product update(Long id, Product updated) {
        Product product = findById(id);
        if (updated.getName() != null) product.setName(updated.getName());
        if (updated.getCategory() != null) product.setCategory(updated.getCategory());
        if (updated.getSizeLabel() != null) product.setSizeLabel(updated.getSizeLabel());
        if (updated.getPrice() != null) product.setPrice(updated.getPrice());
        if (updated.getPrice() != null && updated.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Цената трябва да е положителна");
        }
        product.setAllowsSauce(updated.isAllowsSauce());
        product.setActive(updated.isActive());
        return productRepository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = findById(id);
        product.setActive(false);
        productRepository.save(product);
    }

    @Transactional
    public ProductSale sell(Long productId, int quantity, String sauce, String cashierUsername, Long cinemaId) {
        if (quantity <= 0) {
            throw new BusinessException("Количеството трябва да е поне 1");
        }
        if (cinemaId == null) {
            throw new BusinessException("Избери кино за продажбата");
        }
        Product product = findById(productId);
        if (!product.isActive()) {
            throw new BusinessException("Продуктът не е активен");
        }

        String normalizedSauce = null;
        if (product.isAllowsSauce()) {
            if (sauce == null || sauce.isBlank()) {
                throw new BusinessException("Избери сос: CHEDDAR или BBQ");
            }
            normalizedSauce = sauce.trim().toUpperCase();
            if (!SAUCES.contains(normalizedSauce)) {
                throw new BusinessException("Невалиден сос. Ползвай CHEDDAR или BBQ");
            }
        }

        User cashier = userRepository.findByUsername(cashierUsername)
                .orElseThrow(() -> new BusinessException("Касиерът не е намерен"));
        Cinema cinema = cinemaService.findById(cinemaId);

        BigDecimal unit = product.getPrice().setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = unit.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);

        ProductSale sale = new ProductSale();
        sale.setProduct(product);
        sale.setSoldBy(cashier);
        sale.setCinema(cinema);
        sale.setQuantity(quantity);
        sale.setSauce(normalizedSauce);
        sale.setUnitPrice(unit);
        sale.setTotalPrice(total);
        sale.setSoldAt(LocalDateTime.now());
        return productSaleRepository.save(sale);
    }

    @Transactional
    public List<ProductSale> sellMany(List<ProductSaleRequest> items, String cashierUsername, Long cinemaId) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("Добави поне един продукт в поръчката");
        }
        List<ProductSale> sales = new ArrayList<>();
        for (ProductSaleRequest item : items) {
            int quantity = item.getQuantity() == null ? 1 : item.getQuantity();
            sales.add(sell(item.getProductId(), quantity, item.getSauce(), cashierUsername, cinemaId));
        }
        return sales;
    }

    private void validate(Product product) {
        if (product.getName() == null || product.getName().isBlank()) {
            throw new BusinessException("Името е задължително");
        }
        if (product.getCategory() == null) {
            throw new BusinessException("Категорията е задължителна");
        }
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Цената трябва да е положителна");
        }
        if (product.getCategory() == ProductCategory.NACHOS) {
            product.setAllowsSauce(true);
        }
    }
}
