package com.example.carmarketplace.sample;

import com.example.carmarketplace.api.ApiResponses;
import com.example.carmarketplace.domain.Account;
import com.example.carmarketplace.domain.AccountRole;
import com.example.carmarketplace.domain.AuditEvent;
import com.example.carmarketplace.domain.Car;
import com.example.carmarketplace.domain.PurchaseOrder;
import com.example.carmarketplace.domain.Review;
import com.example.carmarketplace.domain.VehicleType;
import com.example.carmarketplace.domain.WishlistEntry;
import com.example.carmarketplace.repository.AccountRepository;
import com.example.carmarketplace.repository.CarRepository;
import com.example.carmarketplace.service.CarService;
import com.example.carmarketplace.storage.JsonFileStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Makes sure the files in sample-data/ really load and behave with the application code. */
class SampleDataTest {
    @TempDir
    Path dataDirectory;

    private JsonFileStore store;

    @BeforeEach
    void seed() {
        // Uses the same auto-seeding path as a first application start.
        store = new JsonFileStore(new ObjectMapper().findAndRegisterModules(),
                dataDirectory.toString(), true, "sample-data");
    }

    @Test
    void demoAccountsCanAuthenticateWithDocumentedPasswords() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        Map<String, Account> byEmail = store.readAll("users.json", Account.class).stream()
                .collect(Collectors.toMap(Account::getEmail, account -> account));

        assertEquals(7, byEmail.size());
        assertEquals(AccountRole.ADMIN, byEmail.get("admin@goodroad.lk").getRole());
        assertTrue(encoder.matches("AdminPass123!", byEmail.get("admin@goodroad.lk").getPasswordHash()));
        for (Map.Entry<String, Account> entry : byEmail.entrySet()) {
            if (entry.getValue().getRole() != AccountRole.ADMIN) {
                assertTrue(encoder.matches("password123", entry.getValue().getPasswordHash()), entry.getKey());
            }
            assertTrue(entry.getValue().isActive(), entry.getKey());
        }
    }

    @Test
    void everyReferenceInTheSampleFilesPointsToARealRecord() {
        Set<String> accountIds = store.readAll("users.json", Account.class).stream()
                .map(Account::getId).collect(Collectors.toSet());
        Set<String> carIds = store.readAll("cars.json", Car.class).stream()
                .map(Car::getId).collect(Collectors.toSet());
        Map<String, Car> cars = store.readAll("cars.json", Car.class).stream()
                .collect(Collectors.toMap(Car::getId, car -> car));

        store.readAll("cars.json", Car.class)
                .forEach(car -> assertTrue(accountIds.contains(car.getSellerId()), car.getId()));
        for (PurchaseOrder order : store.readAll("orders.json", PurchaseOrder.class)) {
            assertTrue(accountIds.contains(order.getBuyerId()), order.getId());
            assertTrue(carIds.contains(order.getCarId()), order.getId());
            assertEquals(cars.get(order.getCarId()).getSellerId(), order.getSellerId(), order.getId());
        }
        for (WishlistEntry entry : store.readAll("wishlists.json", WishlistEntry.class)) {
            assertTrue(accountIds.contains(entry.getBuyerId()), entry.getId());
            assertTrue(carIds.contains(entry.getCarId()), entry.getId());
        }
        for (Review review : store.readAll("reviews.json", Review.class)) {
            assertTrue(accountIds.contains(review.getBuyerId()), review.getId());
            assertTrue(carIds.contains(review.getCarId()), review.getId());
        }
        for (AuditEvent event : store.readAll("audit.json", AuditEvent.class)) {
            assertTrue(accountIds.contains(event.getActorId()), event.getId());
        }
    }

    @Test
    void carSearchAndFiltersWorkAgainstTheSampleListings() {
        CarService cars = new CarService(new CarRepository(store), new AccountRepository(store));

        // 8 ACTIVE cars; the sold Toyota Axio is hidden from search.
        assertEquals(8, cars.search(null, null, null, null, null, null, null, null, null).size());
        assertEquals(1, cars.search("toyota aqua", null, null, null, null, null, null, null, null).size());
        assertEquals(2, cars.search("toyota", null, null, null, null, null, null, null, null).size());
        assertEquals(4, cars.search(null, VehicleType.HATCHBACK, null, null, null, null, null, null, null).size());
        assertEquals(2, cars.search(null, VehicleType.SUV, null, null, null, null, null, null, null).size());

        List<ApiResponses.CarView> cheap = cars.search(null, null, null, new BigDecimal("5000000"),
                null, null, null, null, "price_asc");
        assertEquals(List.of("Wagon R", "Aqua"),
                cheap.stream().map(ApiResponses.CarView::model).collect(Collectors.toList()));

        assertEquals(1, cars.search(null, null, null, null, 2021, null, null, null, null).size());
        assertEquals(2, cars.search(null, null, null, null, null, null, null, 25000L, "mileage_asc").size());
    }
}
