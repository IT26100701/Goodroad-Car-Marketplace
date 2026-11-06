package com.example.carmarketplace.controller;

import com.example.carmarketplace.api.ApiRequests;
import com.example.carmarketplace.api.ApiResponses;
import com.example.carmarketplace.security.AccountPrincipal;
import com.example.carmarketplace.service.AccountService;
import com.example.carmarketplace.service.CarService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/me")
public class UserController {
    private final AccountService accountService;
    private final CarService carService;

    public UserController(AccountService accountService, CarService carService) {
        this.accountService = accountService;
        this.carService = carService;
    }

    @GetMapping
    public ApiResponses.AccountView profile(@AuthenticationPrincipal AccountPrincipal principal) {
        return ApiResponses.account(accountService.findById(principal.getAccount().getId()));
    }

    @PutMapping
    public ApiResponses.AccountView updateProfile(
            @AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody ApiRequests.ProfileUpdateRequest request) {
        return ApiResponses.account(accountService.updateProfile(principal.getAccount().getId(), request));
    }

    @DeleteMapping
    public ResponseEntity<Void> deactivate(
            @AuthenticationPrincipal AccountPrincipal principal,
            HttpServletRequest request) {
        accountService.deactivate(principal.getAccount().getId());
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/cars")
    @PreAuthorize("hasRole('SELLER')")
    public List<ApiResponses.CarView> myListings(@AuthenticationPrincipal AccountPrincipal principal) {
        return carService.getSellerListings(principal.getAccount().getId());
    }
}
