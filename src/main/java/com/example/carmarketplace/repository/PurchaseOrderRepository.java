package com.example.carmarketplace.repository;

import com.example.carmarketplace.domain.PurchaseOrder;
import com.example.carmarketplace.storage.JsonFileStore;
import org.springframework.stereotype.Repository;

@Repository
public class PurchaseOrderRepository extends JsonListRepository<PurchaseOrder> {
    public PurchaseOrderRepository(JsonFileStore store) {
        super(store, "orders.json", PurchaseOrder.class);
    }
}
