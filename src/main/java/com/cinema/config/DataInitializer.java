package com.cinema.config;

import com.cinema.model.Product;
import com.cinema.model.User;
import com.cinema.model.enums.ProductCategory;
import com.cinema.model.enums.Role;
import com.cinema.repository.ProductRepository;
import com.cinema.repository.UserRepository;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(
            UserRepository userRepository,
            ProductRepository productRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            createUserIfMissing(userRepository, passwordEncoder, "admin", "admin123", Role.ADMIN, "1000.00");
            createUserIfMissing(userRepository, passwordEncoder, "cashier", "cashier123", Role.CASHIER, "0.00");
            createUserIfMissing(userRepository, passwordEncoder, "client", "client123", Role.CLIENT, "50.00");

            // Ако потребителят вече съществува без баланс колона (стар запис), допълваме
            userRepository.findAll().forEach(user -> {
                if (user.getBalance() == null) {
                    user.setBalance(user.getRole() == Role.CLIENT ? new BigDecimal("50.00") : BigDecimal.ZERO);
                    userRepository.save(user);
                }
            });

            seedProduct(productRepository, "Пуканки", ProductCategory.POPCORN, "Малка", "4.50", false);
            seedProduct(productRepository, "Пуканки", ProductCategory.POPCORN, "Средна", "6.50", false);
            seedProduct(productRepository, "Пуканки", ProductCategory.POPCORN, "Голяма", "8.50", false);
            seedProduct(productRepository, "Кола 500мл", ProductCategory.DRINK, "500мл", "3.50", false);
            seedProduct(productRepository, "Спрайт 500мл", ProductCategory.DRINK, "500мл", "3.50", false);
            seedProduct(productRepository, "Фанта 500мл", ProductCategory.DRINK, "500мл", "3.50", false);
            seedProduct(productRepository, "Начос", ProductCategory.NACHOS, "Малък", "7.00", true);
            seedProduct(productRepository, "Начос", ProductCategory.NACHOS, "Голям", "9.50", true);
        };
    }

    private void createUserIfMissing(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            String username,
            String rawPassword,
            Role role,
            String balance
    ) {
        if (!userRepository.existsByUsername(username)) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setRole(role);
            user.setBalance(new BigDecimal(balance));
            userRepository.save(user);
            System.out.println("Създаден потребител: " + username + " / " + rawPassword + " (" + role + ")");
        }
    }

    private void seedProduct(
            ProductRepository productRepository,
            String name,
            ProductCategory category,
            String sizeLabel,
            String price,
            boolean allowsSauce
    ) {
        boolean exists = productRepository.findAll().stream().anyMatch(p ->
                p.getName().equalsIgnoreCase(name)
                        && ((p.getSizeLabel() == null && sizeLabel == null)
                        || (p.getSizeLabel() != null && p.getSizeLabel().equalsIgnoreCase(sizeLabel)))
        );
        if (!exists) {
            Product product = new Product();
            product.setName(name);
            product.setCategory(category);
            product.setSizeLabel(sizeLabel);
            product.setPrice(new BigDecimal(price));
            product.setActive(true);
            product.setAllowsSauce(allowsSauce);
            productRepository.save(product);
        }
    }
}
