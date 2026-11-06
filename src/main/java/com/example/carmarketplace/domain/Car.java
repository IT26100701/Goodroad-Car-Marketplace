package com.example.carmarketplace.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Car {
    private String id;
    private String sellerId;
    private String brand;
    private String model;
    private VehicleType vehicleType;
    private int year;
    private long mileage;
    private BigDecimal price;
    private String description;
    private CarStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private List<String> imageFiles = new ArrayList<>();

    public Car() {
    }

    public Car(String id, String sellerId, String brand, String model, VehicleType vehicleType,
               int year, long mileage, BigDecimal price, String description) {
        this.id = id;
        this.sellerId = sellerId;
        this.brand = brand;
        this.model = model;
        this.vehicleType = vehicleType;
        this.year = year;
        this.mileage = mileage;
        this.price = price;
        this.description = description;
        this.status = CarStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public long getMileage() {
        return mileage;
    }

    public void setMileage(long mileage) {
        this.mileage = mileage;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CarStatus getStatus() {
        return status;
    }

    public void setStatus(CarStatus status) {
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

    public List<String> getImageFiles() {
        return imageFiles;
    }

    public void setImageFiles(List<String> imageFiles) {
        this.imageFiles = imageFiles == null ? new ArrayList<>() : new ArrayList<>(imageFiles);
    }
}
