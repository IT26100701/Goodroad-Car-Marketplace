package com.example.carmarketplace.repository;

import com.example.carmarketplace.domain.AuditEvent;
import com.example.carmarketplace.storage.JsonFileStore;
import org.springframework.stereotype.Repository;

@Repository
public class AuditRepository extends JsonListRepository<AuditEvent> {
    public AuditRepository(JsonFileStore store) {
        super(store, "audit.json", AuditEvent.class);
    }
}
