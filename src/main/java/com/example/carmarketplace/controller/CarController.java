package com.example.carmarketplace.controller;

import com.example.carmarketplace.api.ApiRequests;
import com.example.carmarketplace.api.ApiResponses;
import com.example.carmarketplace.domain.VehicleType;
import com.example.carmarketplace.security.AccountPrincipal;
import com.example.carmarketplace.service.CarService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/cars")
public class CarController {
    private final CarService carService;

    public CarController(CarService carService) {
        this.carService = carService;
    }

    @GetMapping
    public List<ApiResponses.CarView> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) VehicleType vehicleType,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minYear,
            @RequestParam(required = false) Integer maxYear,
            @RequestParam(required = false) Long minMileage,
            @RequestParam(required = false) Long maxMileage,
            @RequestParam(required = false) String sort) {
        return carService.search(q, vehicleType, minPrice, maxPrice, minYear, maxYear, minMileage, maxMileage, sort);
    }

    @GetMapping("/{carId}")
    public ApiResponses.CarView get(@PathVariable String carId) {
        return carService.getPublicCar(carId);
    }

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponses.CarView> create(
            @AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody ApiRequests.CarRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(carService.create(principal.getAccount().getId(), request));
    }

    @PutMapping("/{carId}")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponses.CarView update(
            @PathVariable String carId,
            @AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody ApiRequests.CarRequest request) {
        return carService.update(carId, principal.getAccount().getId(), request);
    }

    @DeleteMapping("/{carId}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> remove(
            @PathVariable String carId,
            @AuthenticationPrincipal AccountPrincipal principal) {
        carService.remove(carId, principal.getAccount().getId());
        return ResponseEntity.noContent().build();
    }
}
