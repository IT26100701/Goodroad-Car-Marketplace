package com.example.carmarketplace.service;

import com.example.carmarketplace.api.ApiResponses;
import com.example.carmarketplace.domain.Account;
import com.example.carmarketplace.domain.AuditEvent;
import com.example.carmarketplace.repository.AccountRepository;
import com.example.carmarketplace.repository.AuditRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class AuditService {
    private final AuditRepository auditRepository;
    private final AccountRepository accountRepository;

    public AuditService(AuditRepository auditRepository, AccountRepository accountRepository) {
        this.auditRepository = auditRepository;
        this.accountRepository = accountRepository;
    }

    public void record(String actorId, String action, String targetType, String targetId, String details) {
        List<AuditEvent> events = auditRepository.findAll();
        events.add(new AuditEvent(UUID.randomUUID().toString(), actorId, action,
                targetType, targetId, details));
        auditRepository.saveAll(events);
    }

    public List<ApiResponses.AuditEventView> list() {
        return auditRepository.findAll().stream()
                .sorted(Comparator.comparing(AuditEvent::getCreatedAt).reversed())
                .map(event -> {
                    Account actor = accountRepository.findAll().stream()
                            .filter(account -> account.getId().equals(event.getActorId()))
                            .findFirst()
                            .orElse(null);
                    return new ApiResponses.AuditEventView(
                            event.getId(),
                            event.getActorId(),
                            actor == null ? "Deleted or deactivated account" : actor.getFullName(),
                            event.getAction(),
                            event.getTargetType(),
                            event.getTargetId(),
                            event.getDetails(),
                            event.getCreatedAt());
                })
                .toList();
    }
}
