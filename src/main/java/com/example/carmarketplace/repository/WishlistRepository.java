package com.example.carmarketplace.repository;

import com.example.carmarketplace.domain.WishlistEntry;
import com.example.carmarketplace.storage.JsonFileStore;
import org.springframework.stereotype.Repository;

@Repository
public class WishlistRepository extends JsonListRepository<WishlistEntry> {
    public WishlistRepository(JsonFileStore store) {
        super(store, "wishlists.json", WishlistEntry.class);
    }
}
