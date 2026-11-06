package com.example.carmarketplace.service;

import com.example.carmarketplace.api.ApiException;
import com.example.carmarketplace.api.ApiRequests;
import com.example.carmarketplace.api.ApiResponses;
import com.example.carmarketplace.domain.Account;
import com.example.carmarketplace.domain.Car;
import com.example.carmarketplace.domain.CarStatus;
import com.example.carmarketplace.domain.VehicleType;
import com.example.carmarketplace.repository.AccountRepository;
import com.example.carmarketplace.repository.CarRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CarService {
    private final CarRepository carRepository;
    private final AccountRepository accountRepository;

    public CarService(CarRepository carRepository, AccountRepository accountRepository) {
        this.carRepository = carRepository;
        this.accountRepository = accountRepository;
    }

    public List<ApiResponses.CarView> search(
            String keyword,
            VehicleType vehicleType,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer minYear,
            Integer maxYear,
            Long minMileage,
            Long maxMileage,
            String sort) {
        validateRange(minPrice, maxPrice, "price");
        validateRange(minYear, maxYear, "year");
        validateRange(minMileage, maxMileage, "mileage");

        List<String> tokens = keyword == null || keyword.isBlank()
                ? List.of()
                : Arrays.stream(keyword.trim().toLowerCase(Locale.ROOT).split("\\s+"))
                        .filter(token -> !token.isEmpty())
                        .toList();
        Comparator<Car> comparator = comparatorFor(sort);
        return carRepository.findAll().stream()
                .filter(car -> car.getStatus() == CarStatus.ACTIVE)
                .filter(car -> matchesKeywords(car, tokens))
                .filter(car -> vehicleType == null || car.getVehicleType() == vehicleType)
                .filter(car -> minPrice == null || car.getPrice().compareTo(minPrice) >= 0)
                .filter(car -> maxPrice == null || car.getPrice().compareTo(maxPrice) <= 0)
                .filter(car -> minYear == null || car.getYear() >= minYear)
                .filter(car -> maxYear == null || car.getYear() <= maxYear)
                .filter(car -> minMileage == null || car.getMileage() >= minMileage)
                .filter(car -> maxMileage == null || car.getMileage() <= maxMileage)
                .sorted(comparator)
                .map(this::toView)
                .toList();
    }

    public ApiResponses.CarView getPublicCar(String id) {
        Car car = findAnyById(id);
        if (car.getStatus() != CarStatus.ACTIVE) {
            throw ApiException.notFound("Available car was not found");
        }
        return toView(car);
    }

    public List<ApiResponses.CarView> getSellerListings(String sellerId) {
        return carRepository.findAll().stream()
                .filter(car -> car.getSellerId().equals(sellerId))
                .sorted(Comparator.comparing(Car::getCreatedAt).reversed())
                .map(this::toView)
                .toList();
    }

    public ApiResponses.CarView create(String sellerId, ApiRequests.CarRequest request) {
        Car car = new Car(UUID.randomUUID().toString(), sellerId, request.brand().trim(),
                request.model().trim(), request.vehicleType(), request.year(), request.mileage(),
                request.price(), request.description().trim());
        save(car);
        return toView(car);
    }

    public ApiResponses.CarView update(String carId, String sellerId, ApiRequests.CarRequest request) {
        Car car = findOwnedActiveCar(carId, sellerId);
        applyRequest(car, request);
        car.setUpdatedAt(Instant.now());
        save(car);
        return toView(car);
    }

    public void remove(String carId, String sellerId) {
        Car car = findOwnedActiveCar(carId, sellerId);
        car.setStatus(CarStatus.REMOVED);
        car.setUpdatedAt(Instant.now());
        save(car);
    }

    public Car findAnyById(String id) {
        return carRepository.findAll().stream()
                .filter(car -> car.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Car was not found"));
    }

    public long countAll() {
        return carRepository.findAll().size();
    }

    public long countActive() {
        return carRepository.findAll().stream().filter(car -> car.getStatus() == CarStatus.ACTIVE).count();
    }

    public void markSold(Car car) {
        car.setStatus(CarStatus.SOLD);
        car.setUpdatedAt(Instant.now());
        save(car);
    }

    public ApiResponses.CarView toView(Car car) {
        String sellerName = accountRepository.findAll().stream()
                .filter(account -> account.getId().equals(car.getSellerId()))
                .map(Account::getFullName)
                .findFirst()
                .orElse("Deactivated seller");
        return new ApiResponses.CarView(
                car.getId(), car.getSellerId(), sellerName, car.getBrand(), car.getModel(),
                car.getVehicleType(), car.getYear(), car.getMileage(), car.getPrice(),
                car.getDescription(), car.getStatus(), car.getCreatedAt(), car.getUpdatedAt(),
                car.getImageFiles().stream().map(file -> "/api/images/" + file).toList());
    }

    private Car findOwnedActiveCar(String carId, String sellerId) {
        Car car = findAnyById(carId);
        if (!car.getSellerId().equals(sellerId)) {
            throw ApiException.forbidden("You can only manage your own car listings");
        }
        if (car.getStatus() != CarStatus.ACTIVE) {
            throw ApiException.conflict("Only an available listing can be changed");
        }
        return car;
    }

    private void applyRequest(Car car, ApiRequests.CarRequest request) {
        car.setBrand(request.brand().trim());
        car.setModel(request.model().trim());
        car.setVehicleType(request.vehicleType());
        car.setYear(request.year());
        car.setMileage(request.mileage());
        car.setPrice(request.price());
        car.setDescription(request.description().trim());
    }

    private void save(Car car) {
        List<Car> cars = carRepository.findAll();
        cars.removeIf(existing -> existing.getId().equals(car.getId()));
        cars.add(car);
        carRepository.saveAll(cars);
    }

    /**
     * Every word typed by the buyer must appear somewhere in the car's brand, model, year,
     * body style or description, so "toyota aqua" and "hybrid 2018" both work.
     */
    private boolean matchesKeywords(Car car, List<String> tokens) {
        if (tokens.isEmpty()) {
            return true;
        }
        String haystack = (text(car.getBrand()) + " " + text(car.getModel()) + " " + car.getYear()
                + " " + text(car.getVehicleType()) + " " + text(car.getDescription()))
                .toLowerCase(Locale.ROOT);
        return tokens.stream().allMatch(haystack::contains);
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private Comparator<Car> comparatorFor(String sort) {
        String sortKey = sort == null || sort.isBlank() ? "newest" : sort.toLowerCase(Locale.ROOT);
        return switch (sortKey) {
            case "price_asc" -> Comparator.comparing(Car::getPrice);
            case "price_desc" -> Comparator.comparing(Car::getPrice).reversed();
            case "year_desc" -> Comparator.comparingInt(Car::getYear).reversed();
            case "mileage_asc" -> Comparator.comparingLong(Car::getMileage);
            case "newest" -> Comparator.comparing(Car::getCreatedAt).reversed();
            default -> throw ApiException.badRequest(
                    "sort must be one of: newest, price_asc, price_desc, year_desc, mileage_asc");
        };
    }

    private <T extends Comparable<T>> void validateRange(T minimum, T maximum, String name) {
        if (minimum != null && maximum != null && minimum.compareTo(maximum) > 0) {
            throw ApiException.badRequest("Minimum " + name + " cannot be greater than maximum " + name);
        }
    }
}
