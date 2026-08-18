package com.cinema.service;

import com.cinema.exception.BusinessException;
import com.cinema.model.User;
import com.cinema.model.enums.Role;
import com.cinema.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Потребителят не е намерен"));
    }

    public Map<String, Object> profile(String username) {
        User user = requireUser(username);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("role", user.getRole().name());
        map.put("balance", user.getBalance());
        return map;
    }

    @Transactional
    public Map<String, Object> updateProfile(
            String currentUsername,
            String newUsername,
            String currentPassword,
            String newPassword
    ) {
        User user = requireUser(currentUsername);
        if (user.getRole() != Role.CLIENT) {
            throw new BusinessException("Само клиентите могат да сменят данни от този екран");
        }

        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException("Текущата парола е грешна");
        }

        if (newUsername != null && !newUsername.isBlank() && !newUsername.equals(user.getUsername())) {
            String trimmed = newUsername.trim();
            if (userRepository.existsByUsername(trimmed)) {
                throw new BusinessException("Това потребителско име вече е заето");
            }
            user.setUsername(trimmed);
        }

        if (newPassword != null && !newPassword.isBlank()) {
            if (newPassword.length() < 4) {
                throw new BusinessException("Новата парола трябва да е поне 4 символа");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        userRepository.save(user);
        return profile(user.getUsername());
    }

    @Transactional
    public Map<String, Object> topUp(String username, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Сумата трябва да е положителна");
        }
        User user = requireUser(username);
        if (user.getRole() != Role.CLIENT) {
            throw new BusinessException("Само клиентите имат баланс за зареждане");
        }
        BigDecimal add = amount.setScale(2, RoundingMode.HALF_UP);
        user.setBalance(user.getBalance().add(add));
        userRepository.save(user);
        return profile(username);
    }

    @Transactional
    public void charge(String username, BigDecimal amount) {
        User user = requireUser(username);
        BigDecimal cost = amount.setScale(2, RoundingMode.HALF_UP);
        if (user.getBalance().compareTo(cost) < 0) {
            throw new BusinessException(
                    "Нямаш достатъчно пари в баланса. Нужни: " + cost + " €, налични: " + user.getBalance() + " €"
            );
        }
        user.setBalance(user.getBalance().subtract(cost));
        userRepository.save(user);
    }
}
