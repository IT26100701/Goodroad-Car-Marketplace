package com.example.carmarketplace.api;

import com.example.carmarketplace.domain.AccountRole;
import com.example.carmarketplace.domain.OrderStatus;
import com.example.carmarketplace.domain.ReviewStatus;
import com.example.carmarketplace.domain.VehicleType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public final class ApiRequests {
    private ApiRequests() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 120) String fullName,
            @Size(max = 30) String phone,
            @NotNull AccountRole role) {
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 72) String password) {
    }

    public record ProfileUpdateRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 120) String fullName,
            @Size(max = 30) String phone) {
    }

    public record AdminBootstrapRequest(
            @NotBlank @Size(min = 32, max = 512) String setupToken,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 12, max = 72) String password,
            @NotBlank @Size(max = 120) String fullName,
            @Size(max = 30) String phone) {
    }

    public record AdminCreateRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 12, max = 72) String password,
            @NotBlank @Size(max = 120) String fullName,
            @Size(max = 30) String phone) {
    }

    public record CarRequest(
            @NotBlank @Size(max = 80) String brand,
            @NotBlank @Size(max = 100) String model,
            @NotNull VehicleType vehicleType,
            @Min(1886) int year,
            @Min(0) long mileage,
            @NotNull @DecimalMin(value = "0.01") BigDecimal price,
            @NotBlank @Size(max = 2000) String description) {
    }

    public record CreateOrderRequest(@NotBlank String carId) {
    }

    public record OrderStatusRequest(@NotNull OrderStatus status) {
    }

    public record ReviewRequest(
            @Min(1) @Max(5) int rating,
            @NotBlank @Size(max = 2000) String comment) {
    }

    public record ReviewModerationRequest(@NotNull ReviewStatus status) {
    }
}
