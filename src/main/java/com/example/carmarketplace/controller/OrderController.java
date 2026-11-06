package com.example.carmarketplace.controller;

import com.example.carmarketplace.api.ApiRequests;
import com.example.carmarketplace.api.ApiResponses;
import com.example.carmarketplace.security.AccountPrincipal;
import com.example.carmarketplace.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasAnyRole('BUYER', 'SELLER', 'ADMIN')")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ApiResponses.OrderView> create(
            @AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody ApiRequests.CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.create(principal.getAccount().getId(), request));
    }

    @GetMapping
    public List<ApiResponses.OrderView> history(@AuthenticationPrincipal AccountPrincipal principal) {
        return orderService.history(principal.getAccount().getId(), principal.getAccount().getRole());
    }

    @PatchMapping("/{orderId}/status")
    public ApiResponses.OrderView updateStatus(
            @PathVariable String orderId,
            @AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody ApiRequests.OrderStatusRequest request) {
        return orderService.updateStatus(orderId, principal.getAccount().getId(),
                principal.getAccount().getRole(), request.status());
    }
}
