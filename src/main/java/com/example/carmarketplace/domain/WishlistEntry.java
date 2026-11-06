package com.example.carmarketplace.domain;

import java.time.Instant;

public class WishlistEntry {
    private String id;
    private String buyerId;
    private String carId;
    private Instant createdAt;

    public WishlistEntry() {
    }

    public WishlistEntry(String id, String buyerId, String carId) {
        this.id = id;
        this.buyerId = buyerId;
        this.carId = carId;
        this.createdAt = Instant.now();
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

    public String getCarId() {
        return carId;
    }

    public void setCarId(String carId) {
        this.carId = carId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
