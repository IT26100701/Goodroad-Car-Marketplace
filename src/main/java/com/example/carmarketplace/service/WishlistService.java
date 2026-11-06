package com.example.carmarketplace.service;

import com.example.carmarketplace.api.ApiException;
import com.example.carmarketplace.api.ApiResponses;
import com.example.carmarketplace.domain.Account;
import com.example.carmarketplace.domain.AccountRole;
import com.example.carmarketplace.domain.Car;
import com.example.carmarketplace.domain.CarStatus;
import com.example.carmarketplace.domain.WishlistEntry;
import com.example.carmarketplace.repository.WishlistRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class WishlistService {
    private final WishlistRepository wishlistRepository;
    private final AccountService accountService;
    private final CarService carService;

    public WishlistService(WishlistRepository wishlistRepository,
                           AccountService accountService,
                           CarService carService) {
        this.wishlistRepository = wishlistRepository;
        this.accountService = accountService;
        this.carService = carService;
    }

    public List<ApiResponses.WishlistItemView> list(String buyerId) {
        requireBuyer(buyerId);
        return wishlistRepository.findAll().stream()
                .filter(entry -> entry.getBuyerId().equals(buyerId))
                .sorted(Comparator.comparing(WishlistEntry::getCreatedAt).reversed())
                .map(entry -> {
                    Car car = carService.findAnyById(entry.getCarId());
                    return new ApiResponses.WishlistItemView(
                            entry.getId(), entry.getCreatedAt(), carService.toView(car));
                })
                .toList();
    }

    public ApiResponses.WishlistItemView add(String buyerId, String carId) {
        requireBuyer(buyerId);
        Car car = carService.findAnyById(carId);
        if (car.getStatus() != CarStatus.ACTIVE) {
            throw ApiException.conflict("Only available cars can be added to a wishlist");
        }
        List<WishlistEntry> entries = wishlistRepository.findAll();
        boolean alreadyAdded = entries.stream()
                .anyMatch(entry -> entry.getBuyerId().equals(buyerId) && entry.getCarId().equals(carId));
        if (alreadyAdded) {
            throw ApiException.conflict("This car is already in your wishlist");
        }

        WishlistEntry entry = new WishlistEntry(UUID.randomUUID().toString(), buyerId, carId);
        entries.add(entry);
        wishlistRepository.saveAll(entries);
        return new ApiResponses.WishlistItemView(entry.getId(), entry.getCreatedAt(), carService.toView(car));
    }

    public void remove(String buyerId, String carId) {
        requireBuyer(buyerId);
        List<WishlistEntry> entries = wishlistRepository.findAll();
        boolean removed = entries.removeIf(entry -> entry.getBuyerId().equals(buyerId)
                && entry.getCarId().equals(carId));
        if (!removed) {
            throw ApiException.notFound("That car is not in your wishlist");
        }
        wishlistRepository.saveAll(entries);
    }

    private void requireBuyer(String buyerId) {
        Account account = accountService.findById(buyerId);
        if (account.getRole() != AccountRole.BUYER) {
            throw ApiException.forbidden("Only buyer accounts can manage wishlists");
        }
    }
}
