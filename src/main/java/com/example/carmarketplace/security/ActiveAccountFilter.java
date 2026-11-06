package com.example.carmarketplace.security;

import com.example.carmarketplace.domain.Account;
import com.example.carmarketplace.repository.AccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

public class ActiveAccountFilter extends OncePerRequestFilter {
    private final AccountRepository accountRepository;

    public ActiveAccountFilter(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            Optional<Account> currentAccount = accountRepository.findAll().stream()
                    .filter(account -> account.getId().equals(principal.getAccount().getId()))
                    .findFirst();
            if (currentAccount.isEmpty() || !currentAccount.get().isActive()) {
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
            } else {
                UserDetails currentPrincipal = new AccountPrincipal(currentAccount.get());
                Authentication refreshed = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                        .authenticated(currentPrincipal, authentication.getCredentials(),
                                currentPrincipal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(refreshed);
            }
        }
        filterChain.doFilter(request, response);
    }
}
