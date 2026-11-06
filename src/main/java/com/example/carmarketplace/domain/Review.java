package com.example.carmarketplace.domain;

import java.time.Instant;

public class Review {
    private String id;
    private String buyerId;
    private String carId;
    private int rating;
    private String comment;
    private ReviewStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public Review() {
    }

    public Review(String id, String buyerId, String carId, int rating, String comment) {
        this.id = id;
        this.buyerId = buyerId;
        this.carId = carId;
        this.rating = rating;
        this.comment = comment;
        this.status = ReviewStatus.PUBLISHED;
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

    public String getCarId() {
        return carId;
    }

    public void setCarId(String carId) {
        this.carId = carId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
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
