package com.cinema.controller;

import com.cinema.dto.ProductRequest;
import com.cinema.dto.ProductSaleBatchRequest;
import com.cinema.dto.ProductSaleRequest;
import com.cinema.model.Product;
import com.cinema.model.ProductSale;
import com.cinema.service.ProductService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public List<Product> activeProducts() {
        return productService.findActive();
    }

    @GetMapping("/admin/products")
    public List<Product> allProducts() {
        return productService.findAll();
    }

    @PostMapping("/admin/products")
    public ResponseEntity<Product> create(@RequestBody ProductRequest request) {
        Product product = fromRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(product));
    }

    @PutMapping("/admin/products/{id}")
    public Product update(@PathVariable Long id, @RequestBody ProductRequest request) {
        Product existing = productService.findById(id);
        if (request.getName() != null) existing.setName(request.getName());
        if (request.getCategory() != null) existing.setCategory(request.getCategory());
        if (request.getSizeLabel() != null) existing.setSizeLabel(request.getSizeLabel());
        if (request.getPrice() != null) existing.setPrice(request.getPrice());
        if (request.getActive() != null) existing.setActive(request.getActive());
        if (request.getAllowsSauce() != null) existing.setAllowsSauce(request.getAllowsSauce());
        return productService.update(id, existing);
    }

    @DeleteMapping("/admin/products/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cashier/products/sell")
    public ResponseEntity<ProductSale> sell(
            @RequestBody ProductSaleRequest request,
            Authentication authentication
    ) {
        ProductSale sale = productService.sell(
                request.getProductId(),
                request.getQuantity() == null ? 1 : request.getQuantity(),
                request.getSauce(),
                authentication.getName(),
                request.getCinemaId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(sale);
    }

    @PostMapping("/cashier/products/sell-batch")
    public ResponseEntity<List<ProductSale>> sellBatch(
            @RequestBody ProductSaleBatchRequest request,
            Authentication authentication
    ) {
        List<ProductSale> sales = productService.sellMany(
                request.getItems(),
                authentication.getName(),
                request.getCinemaId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(sales);
    }

    private Product fromRequest(ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setCategory(request.getCategory());
        product.setSizeLabel(request.getSizeLabel());
        product.setPrice(request.getPrice());
        product.setAllowsSauce(Boolean.TRUE.equals(request.getAllowsSauce()));
        product.setActive(request.getActive() == null || request.getActive());
        return product;
    }
}
