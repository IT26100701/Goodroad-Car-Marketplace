package com.example.carmarketplace.service;

import com.example.carmarketplace.api.ApiException;
import com.example.carmarketplace.api.ApiRequests;
import com.example.carmarketplace.api.ApiResponses;
import com.example.carmarketplace.domain.Account;
import com.example.carmarketplace.domain.AccountRole;
import com.example.carmarketplace.domain.AdminAccount;
import com.example.carmarketplace.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Service
public class AdminService {
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final CarService carService;
    private final OrderService orderService;
    private final ReviewService reviewService;
    private final AuditService auditService;
    private final String setupToken;

    public AdminService(AccountRepository accountRepository,
                        AccountService accountService,
                        CarService carService,
                        OrderService orderService,
                        ReviewService reviewService,
                        AuditService auditService,
                        @Value("${app.admin.setup-token:}") String setupToken) {
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.carService = carService;
        this.orderService = orderService;
        this.reviewService = reviewService;
        this.auditService = auditService;
        this.setupToken = setupToken;
    }

    public synchronized ApiResponses.AccountView bootstrap(ApiRequests.AdminBootstrapRequest request) {
        if (setupToken.isBlank()) {
            throw ApiException.unavailable(
                    "Initial administrator setup is disabled; configure APP_ADMIN_SETUP_TOKEN first");
        }
        boolean matches = MessageDigest.isEqual(
                setupToken.getBytes(StandardCharsets.UTF_8),
                request.setupToken().getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            throw ApiException.forbidden("Administrator setup token is invalid");
        }
        boolean administratorExists = accountRepository.findAll().stream()
                .anyMatch(account -> account.getRole() == AccountRole.ADMIN);
        if (administratorExists) {
            throw ApiException.conflict("Initial administrator setup has already been completed");
        }
        Account admin = accountService.createAdmin(
                request.email(), request.password(), request.fullName(), request.phone());
        return ApiResponses.account(admin);
    }

    public ApiResponses.AccountView createAdmin(String actorId, ApiRequests.AdminCreateRequest request) {
        Account admin = accountService.createAdmin(
                request.email(), request.password(), request.fullName(), request.phone());
        auditService.record(actorId, "ADMIN_CREATED", "account", admin.getId(),
                "Administrator account created");
        return ApiResponses.account(admin);
    }

    public List<ApiResponses.AccountView> listAdmins() {
        return accountRepository.findAll().stream()
                .filter(account -> account.getRole() == AccountRole.ADMIN)
                .map(ApiResponses::account)
                .toList();
    }

    public List<ApiResponses.AccountView> listUsers() {
        return accountService.findAll().stream()
                .map(ApiResponses::account)
                .toList();
    }

    public void deleteAdmin(String adminId, String actorId) {
        if (adminId.equals(actorId)) {
            throw ApiException.conflict("An administrator cannot deactivate their own account here");
        }
        Account target = accountService.findById(adminId);
        if (target.getRole() != AccountRole.ADMIN) {
            throw ApiException.badRequest("The selected account is not an administrator");
        }
        accountService.deactivate(adminId);
        auditService.record(actorId, "ADMIN_DEACTIVATED", "account", adminId,
                "Administrator account deactivated");
    }

    public void deactivateUser(String targetId, String actorId) {
        if (targetId.equals(actorId)) {
            throw ApiException.conflict("Use the account self-deactivation endpoint for your own account");
        }
        Account target = accountService.deactivate(targetId);
        auditService.record(actorId, "USER_DEACTIVATED", "account", targetId,
                "Account deactivated (" + target.getRole() + ")");
    }

    public ApiResponses.AdminOverview overview() {
        List<Account> accounts = accountRepository.findAll();
        return new ApiResponses.AdminOverview(
                accounts.size(),
                accounts.stream().filter(Account::isActive).count(),
                carService.countAll(),
                carService.countActive(),
                orderService.countAll(),
                orderService.countPending(),
                reviewService.countAll(),
                reviewService.countPublished());
    }
}
