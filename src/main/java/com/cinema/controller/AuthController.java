package com.cinema.controller;

import com.cinema.dto.RegisterRequest;
import com.cinema.dto.StaffCreateRequest;
import com.cinema.model.User;
import com.cinema.service.AuthService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        User user = authService.registerClient(request.getUsername(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(toMap(user));
    }

    @GetMapping("/admin/staff")
    public List<Map<String, Object>> listStaff() {
        return authService.listAllUsers().stream().map(this::toMap).toList();
    }

    @PostMapping("/admin/staff")
    public ResponseEntity<Map<String, Object>> createStaff(@RequestBody StaffCreateRequest request) {
        User user = authService.createStaff(
                request.getUsername(),
                request.getPassword(),
                request.getRole()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toMap(user));
    }

    @DeleteMapping("/admin/staff/{id}")
    public Map<String, Object> deleteStaff(@PathVariable Long id, Authentication authentication) {
        return authService.deleteUser(id, authentication.getName());
    }

    private Map<String, Object> toMap(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("role", user.getRole().name());
        return map;
    }
}
