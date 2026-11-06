package com.example.carmarketplace.controller;

import com.example.carmarketplace.api.ApiRequests;
import com.example.carmarketplace.api.ApiResponses;
import com.example.carmarketplace.security.AccountPrincipal;
import com.example.carmarketplace.service.AdminService;
import com.example.carmarketplace.service.AuditService;
import com.example.carmarketplace.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;
    private final AuditService auditService;
    private final ReviewService reviewService;

    public AdminController(AdminService adminService, AuditService auditService, ReviewService reviewService) {
        this.adminService = adminService;
        this.auditService = auditService;
        this.reviewService = reviewService;
    }

    @PostMapping("/bootstrap")
    public ResponseEntity<ApiResponses.AccountView> bootstrap(
            @Valid @RequestBody ApiRequests.AdminBootstrapRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.bootstrap(request));
    }

    @PostMapping("/admins")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponses.AccountView> createAdmin(
            @AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody ApiRequests.AdminCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.createAdmin(principal.getAccount().getId(), request));
    }

    @GetMapping("/admins")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ApiResponses.AccountView> admins() {
        return adminService.listAdmins();
    }

    @DeleteMapping("/admins/{adminId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAdmin(
            @PathVariable String adminId,
            @AuthenticationPrincipal AccountPrincipal principal) {
        adminService.deleteAdmin(adminId, principal.getAccount().getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ApiResponses.AccountView> users() {
        return adminService.listUsers();
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateUser(
            @PathVariable String userId,
            @AuthenticationPrincipal AccountPrincipal principal) {
        adminService.deactivateUser(userId, principal.getAccount().getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponses.AdminOverview overview() {
        return adminService.overview();
    }

    @GetMapping("/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ApiResponses.AuditEventView> audit() {
        return auditService.list();
    }

    @GetMapping("/reviews")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ApiResponses.ReviewView> reviews() {
        return reviewService.listAllForAdmin();
    }

    @PatchMapping("/reviews/{reviewId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponses.ReviewView moderateReview(
            @PathVariable String reviewId,
            @AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody ApiRequests.ReviewModerationRequest request) {
        return reviewService.moderate(reviewId, principal.getAccount().getId(), request);
    }
}
