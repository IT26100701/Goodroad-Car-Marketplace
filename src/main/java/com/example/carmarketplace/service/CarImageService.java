package com.example.carmarketplace.service;

import com.example.carmarketplace.api.ApiException;
import com.example.carmarketplace.api.ApiResponses;
import com.example.carmarketplace.domain.Car;
import com.example.carmarketplace.domain.CarStatus;
import com.example.carmarketplace.repository.CarRepository;
import com.example.carmarketplace.storage.ImageStorage;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class CarImageService {
    public static final int MAX_IMAGES_PER_CAR = 6;

    private final CarRepository carRepository;
    private final CarService carService;
    private final ImageStorage imageStorage;

    public CarImageService(CarRepository carRepository, CarService carService, ImageStorage imageStorage) {
        this.carRepository = carRepository;
        this.carService = carService;
        this.imageStorage = imageStorage;
    }

    public synchronized ApiResponses.CarView add(String carId, String sellerId, List<byte[]> uploads) {
        if (uploads == null || uploads.isEmpty()) {
            throw ApiException.badRequest("Choose at least one image to upload");
        }
        List<Car> cars = carRepository.findAll();
        Car car = ownedActiveCar(cars, carId, sellerId);
        if (car.getImageFiles().size() + uploads.size() > MAX_IMAGES_PER_CAR) {
            throw ApiException.conflict("A listing can have at most " + MAX_IMAGES_PER_CAR + " photos");
        }

        List<String> savedFiles = new ArrayList<>();
        try {
            for (byte[] data : uploads) {
                savedFiles.add(imageStorage.save(data));
            }
        } catch (RuntimeException exception) {
            savedFiles.forEach(imageStorage::delete);
            throw exception;
        }

        car.getImageFiles().addAll(savedFiles);
        car.setUpdatedAt(Instant.now());
        carRepository.saveAll(cars);
        return carService.toView(car);
    }

    public synchronized ApiResponses.CarView remove(String carId, String sellerId, String fileName) {
        List<Car> cars = carRepository.findAll();
        Car car = ownedActiveCar(cars, carId, sellerId);
        if (!car.getImageFiles().remove(fileName)) {
            throw ApiException.notFound("That photo is not part of this listing");
        }
        car.setUpdatedAt(Instant.now());
        carRepository.saveAll(cars);
        imageStorage.delete(fileName);
        return carService.toView(car);
    }

    private Car ownedActiveCar(List<Car> cars, String carId, String sellerId) {
        Car car = cars.stream()
                .filter(existing -> existing.getId().equals(carId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Car was not found"));
        if (!car.getSellerId().equals(sellerId)) {
            throw ApiException.forbidden("You can only manage your own car listings");
        }
        if (car.getStatus() != CarStatus.ACTIVE) {
            throw ApiException.conflict("Only an available listing can be changed");
        }
        return car;
    }
}
