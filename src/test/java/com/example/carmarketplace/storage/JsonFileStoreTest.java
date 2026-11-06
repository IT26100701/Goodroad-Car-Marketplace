package com.example.carmarketplace.storage;

import com.example.carmarketplace.domain.Account;
import com.example.carmarketplace.domain.BuyerAccount;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class JsonFileStoreTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void storesAndRestoresPolymorphicAccounts() {
        JsonFileStore store = new JsonFileStore(new ObjectMapper().findAndRegisterModules(),
                temporaryDirectory.toString());
        BuyerAccount buyer = new BuyerAccount(
                "buyer-1", "buyer@example.test", "$2a$12$test-hash", "Test Buyer", null);
        buyer.setCreatedAt(Instant.parse("2026-10-27T10:00:00Z"));

        store.writeAll("users.json", List.of(buyer), Account.class);
        List<Account> restored = store.readAll("users.json", Account.class);

        assertEquals(1, restored.size());
        assertInstanceOf(BuyerAccount.class, restored.get(0));
        assertEquals("buyer@example.test", restored.get(0).getEmail());
        assertEquals(Instant.parse("2026-10-27T10:00:00Z"), restored.get(0).getCreatedAt());
        assertEquals("BUYER", restored.get(0).getRole().name());
    }

    @Test
    void missingCollectionStartsEmpty() {
        JsonFileStore store = new JsonFileStore(new ObjectMapper().findAndRegisterModules(),
                temporaryDirectory.toString());

        assertEquals(List.of(), store.readAll("cars.json", String.class));
    }
}
