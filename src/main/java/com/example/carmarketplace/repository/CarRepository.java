package com.example.carmarketplace.repository;

import com.example.carmarketplace.domain.Car;
import com.example.carmarketplace.storage.JsonFileStore;
import org.springframework.stereotype.Repository;

@Repository
public class CarRepository extends JsonListRepository<Car> {
    public CarRepository(JsonFileStore store) {
        super(store, "cars.json", Car.class);
    }
}
