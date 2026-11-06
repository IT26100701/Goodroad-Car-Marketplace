package com.example.carmarketplace.repository;

import com.example.carmarketplace.domain.Review;
import com.example.carmarketplace.storage.JsonFileStore;
import org.springframework.stereotype.Repository;

@Repository
public class ReviewRepository extends JsonListRepository<Review> {
    public ReviewRepository(JsonFileStore store) {
        super(store, "reviews.json", Review.class);
    }
}
