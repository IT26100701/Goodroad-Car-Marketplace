package com.example.carmarketplace.repository;

import com.example.carmarketplace.storage.JsonFileStore;

import java.util.List;

abstract class JsonListRepository<T> {
    private final JsonFileStore store;
    private final String fileName;
    private final Class<T> itemType;

    JsonListRepository(JsonFileStore store, String fileName, Class<T> itemType) {
        this.store = store;
        this.fileName = fileName;
        this.itemType = itemType;
    }

    public List<T> findAll() {
        return store.readAll(fileName, itemType);
    }

    public void saveAll(List<T> values) {
        store.writeAll(fileName, values, itemType);
    }
}
