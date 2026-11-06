package com.example.carmarketplace.controller;

import com.example.carmarketplace.api.ApiResponses;
import com.example.carmarketplace.security.AccountPrincipal;
import com.example.carmarketplace.service.WishlistService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@PreAuthorize("hasRole('BUYER')")
public class WishlistController {
    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public List<ApiResponses.WishlistItemView> list(@AuthenticationPrincipal AccountPrincipal principal) {
        return wishlistService.list(principal.getAccount().getId());
    }

    @PostMapping("/{carId}")
    public ResponseEntity<ApiResponses.WishlistItemView> add(
            @PathVariable String carId,
            @AuthenticationPrincipal AccountPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(wishlistService.add(principal.getAccount().getId(), carId));
    }

    @DeleteMapping("/{carId}")
    public ResponseEntity<Void> remove(
            @PathVariable String carId,
            @AuthenticationPrincipal AccountPrincipal principal) {
        wishlistService.remove(principal.getAccount().getId(), carId);
        return ResponseEntity.noContent().build();
    }
}
