package com.example.carmarketplace.api;

import com.example.carmarketplace.domain.Account;
import com.example.carmarketplace.domain.AccountRole;
import com.example.carmarketplace.domain.CarStatus;
import com.example.carmarketplace.domain.OrderStatus;
import com.example.carmarketplace.domain.ReviewStatus;
import com.example.carmarketplace.domain.VehicleType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ApiResponses {
    private ApiResponses() {
    }

    public static AccountView account(Account account) {
        return new AccountView(account.getId(), account.getEmail(), account.getFullName(),
                account.getPhone(), account.getRole(), account.isActive(), account.getCreatedAt());
    }

    public record AccountView(
            String id,
            String email,
            String fullName,
            String phone,
            AccountRole role,
            boolean active,
            Instant createdAt) {
    }

    public record CarView(
            String id,
            String sellerId,
            String sellerName,
            String brand,
            String model,
            VehicleType vehicleType,
            int year,
            long mileage,
            BigDecimal price,
            String description,
            CarStatus status,
            Instant createdAt,
            Instant updatedAt,
            List<String> images) {
    }

    public record OrderView(
            String id,
            String buyerId,
            String buyerName,
            String sellerId,
            String sellerName,
            String carId,
            String carLabel,
            BigDecimal amount,
            OrderStatus status,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record WishlistItemView(
            String id,
            Instant addedAt,
            CarView car) {
    }

    public record ReviewView(
            String id,
            String buyerId,
            String buyerName,
            String carId,
            int rating,
            String comment,
            ReviewStatus status,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record AdminOverview(
            long totalUsers,
            long activeUsers,
            long totalCars,
            long activeCars,
            long totalTransactions,
            long pendingTransactions,
            long totalReviews,
            long publishedReviews) {
    }

    public record AuditEventView(
            String id,
            String actorId,
            String actorName,
            String action,
            String targetType,
            String targetId,
            String details,
            Instant createdAt) {
    }
}
