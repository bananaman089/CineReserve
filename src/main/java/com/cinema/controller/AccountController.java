package com.cinema.controller;

import com.cinema.dto.ProfileUpdateRequest;
import com.cinema.dto.TopUpRequest;
import com.cinema.service.AccountService;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        return accountService.profile(authentication.getName());
    }

    @PutMapping("/profile")
    public Map<String, Object> updateProfile(
            @RequestBody ProfileUpdateRequest request,
            Authentication authentication
    ) {
        return accountService.updateProfile(
                authentication.getName(),
                request.getUsername(),
                request.getCurrentPassword(),
                request.getNewPassword()
        );
    }

    @PostMapping("/top-up")
    public Map<String, Object> topUp(@RequestBody TopUpRequest request, Authentication authentication) {
        return accountService.topUp(authentication.getName(), request.getAmount());
    }
}
