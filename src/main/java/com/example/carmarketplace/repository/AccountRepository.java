package com.example.carmarketplace.repository;

import com.example.carmarketplace.domain.Account;
import com.example.carmarketplace.storage.JsonFileStore;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepository extends JsonListRepository<Account> {
    public AccountRepository(JsonFileStore store) {
        super(store, "users.json", Account.class);
    }
}
