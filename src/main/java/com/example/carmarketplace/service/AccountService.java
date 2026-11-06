package com.example.carmarketplace.service;

import com.example.carmarketplace.api.ApiException;
import com.example.carmarketplace.api.ApiRequests;
import com.example.carmarketplace.domain.Account;
import com.example.carmarketplace.domain.AccountRole;
import com.example.carmarketplace.domain.AdminAccount;
import com.example.carmarketplace.domain.BuyerAccount;
import com.example.carmarketplace.domain.SellerAccount;
import com.example.carmarketplace.repository.AccountRepository;
import com.example.carmarketplace.security.AccountPrincipal;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AccountService implements UserDetailsService {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Account register(ApiRequests.RegisterRequest request) {
        AccountRole role = request.role();
        if (role == AccountRole.ADMIN) {
            throw ApiException.badRequest("Public registration is limited to buyer and seller accounts");
        }
        ensureEmailAvailable(request.email(), null);
        String password = validatedPassword(request.password());
        String id = UUID.randomUUID().toString();
        String email = normalizeEmail(request.email());
        String encodedPassword = passwordEncoder.encode(password);
        return saveAccount(role == AccountRole.BUYER
                ? new BuyerAccount(id, email, encodedPassword, request.fullName().trim(), cleanPhone(request.phone()))
                : new SellerAccount(id, email, encodedPassword, request.fullName().trim(), cleanPhone(request.phone())));
    }

    public Account createAdmin(String email, String password, String fullName, String phone) {
        ensureEmailAvailable(email, null);
        Account admin = new AdminAccount(UUID.randomUUID().toString(), normalizeEmail(email),
                passwordEncoder.encode(validatedPassword(password)), fullName.trim(), cleanPhone(phone));
        return saveAccount(admin);
    }

    public Account findById(String id) {
        return accountRepository.findAll().stream()
                .filter(account -> account.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Account was not found"));
    }

    public Account findByEmail(String email) {
        String normalized = normalizeEmail(email);
        return accountRepository.findAll().stream()
                .filter(account -> account.getEmail().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Account was not found"));
    }

    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    public Account updateProfile(String id, ApiRequests.ProfileUpdateRequest request) {
        Account account = findById(id);
        ensureEmailAvailable(request.email(), id);
        account.setEmail(normalizeEmail(request.email()));
        account.setFullName(request.fullName().trim());
        account.setPhone(cleanPhone(request.phone()));
        saveAccount(account);
        return account;
    }

    public Account deactivate(String id) {
        Account account = findById(id);
        if (!account.isActive()) {
            return account;
        }
        if (account.getRole() == AccountRole.ADMIN) {
            long activeAdmins = accountRepository.findAll().stream()
                    .filter(existing -> existing.getRole() == AccountRole.ADMIN && existing.isActive())
                    .count();
            if (activeAdmins <= 1) {
                throw ApiException.conflict("The last active administrator cannot be deactivated");
            }
        }
        account.setActive(false);
        saveAccount(account);
        return account;
    }

    @Override
    public AccountPrincipal loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalized = normalizeEmail(email);
        return accountRepository.findAll().stream()
                .filter(account -> account.getEmail().equalsIgnoreCase(normalized))
                .findFirst()
                .map(AccountPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
    }

    public Account saveAccount(Account account) {
        List<Account> accounts = accountRepository.findAll();
        accounts.removeIf(existing -> existing.getId().equals(account.getId()));
        accounts.add(account);
        accountRepository.saveAll(accounts);
        return account;
    }

    private void ensureEmailAvailable(String email, String exceptId) {
        String normalized = normalizeEmail(email);
        boolean exists = accountRepository.findAll().stream()
                .anyMatch(account -> account.getEmail().equalsIgnoreCase(normalized)
                        && !account.getId().equals(exceptId));
        if (exists) {
            throw ApiException.conflict("An account with this email already exists");
        }
    }

    private String validatedPassword(String password) {
        if (password == null || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw ApiException.badRequest("Password must be no longer than 72 UTF-8 bytes");
        }
        return password;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String cleanPhone(String phone) {
        return phone == null || phone.isBlank() ? null : phone.trim();
    }
}
