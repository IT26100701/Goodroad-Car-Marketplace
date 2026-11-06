package com.example.carmarketplace.controller;

import com.example.carmarketplace.api.ApiRequests;
import com.example.carmarketplace.api.ApiResponses;
import com.example.carmarketplace.security.AccountPrincipal;
import com.example.carmarketplace.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/api/cars/{carId}/reviews")
    public List<ApiResponses.ReviewView> listForCar(@PathVariable String carId) {
        return reviewService.listPublishedForCar(carId);
    }

    @PostMapping("/api/cars/{carId}/reviews")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ApiResponses.ReviewView> create(
            @PathVariable String carId,
            @AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody ApiRequests.ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                reviewService.create(principal.getAccount().getId(), carId, request));
    }

    @PutMapping("/api/reviews/{reviewId}")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponses.ReviewView update(
            @PathVariable String reviewId,
            @AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody ApiRequests.ReviewRequest request) {
        return reviewService.update(reviewId, principal.getAccount().getId(), request);
    }

    @DeleteMapping("/api/reviews/{reviewId}")
    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable String reviewId,
            @AuthenticationPrincipal AccountPrincipal principal) {
        reviewService.delete(reviewId, principal.getAccount().getId(), principal.getAccount().getRole());
        return ResponseEntity.noContent().build();
    }
}
