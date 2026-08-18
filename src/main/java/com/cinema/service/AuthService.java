package com.cinema.service;

import com.cinema.exception.BusinessException;
import com.cinema.model.User;
import com.cinema.model.enums.Role;
import com.cinema.repository.ProductSaleRepository;
import com.cinema.repository.TicketRepository;
import com.cinema.repository.UserRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final ProductSaleRepository productSaleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            TicketRepository ticketRepository,
            ProductSaleRepository productSaleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.productSaleRepository = productSaleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerClient(String username, String rawPassword) {
        return createUser(username, rawPassword, Role.CLIENT, new BigDecimal("20.00"));
    }

    @Transactional
    public User createStaff(String username, String rawPassword, Role role) {
        if (role != Role.ADMIN && role != Role.CASHIER) {
            throw new BusinessException("Можеш да създаваш само ADMIN или CASHIER акаунти");
        }
        return createUser(username, rawPassword, role, BigDecimal.ZERO);
    }

    public List<User> listStaff() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN || u.getRole() == Role.CASHIER)
                .toList();
    }

    public List<User> listAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public Map<String, Object> deleteUser(Long userId, String currentUsername) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Акаунтът не е намерен"));

        boolean selfDeleted = target.getUsername().equals(currentUsername);

        ticketRepository.deleteByUserId(userId);
        productSaleRepository.deleteBySoldById(userId);
        userRepository.delete(target);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deleted", true);
        result.put("selfDeleted", selfDeleted);
        result.put("username", target.getUsername());
        return result;
    }

    private User createUser(String username, String rawPassword, Role role, BigDecimal balance) {
        if (username == null || username.isBlank()) {
            throw new BusinessException("Потребителското име е задължително");
        }
        if (rawPassword == null || rawPassword.length() < 4) {
            throw new BusinessException("Паролата трябва да е поне 4 символа");
        }
        if (userRepository.existsByUsername(username.trim())) {
            throw new BusinessException("Това потребителско име вече е заето");
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setBalance(balance);
        return userRepository.save(user);
    }
}
