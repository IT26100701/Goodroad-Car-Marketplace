package com.example.carmarketplace.domain;

import java.math.BigDecimal;
import java.time.Instant;

public class PurchaseOrder {
    private String id;
    private String buyerId;
    private String sellerId;
    private String carId;
    private BigDecimal amount;
    private OrderStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public PurchaseOrder() {
    }

    public PurchaseOrder(String id, String buyerId, String sellerId, String carId, BigDecimal amount) {
        this.id = id;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.carId = carId;
        this.amount = amount;
        this.status = OrderStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(String buyerId) {
        this.buyerId = buyerId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getCarId() {
        return carId;
    }

    public void setCarId(String carId) {
        this.carId = carId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
