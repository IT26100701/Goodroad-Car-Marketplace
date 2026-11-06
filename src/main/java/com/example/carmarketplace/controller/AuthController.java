package com.example.carmarketplace.controller;

import com.example.carmarketplace.api.ApiRequests;
import com.example.carmarketplace.api.ApiResponses;
import com.example.carmarketplace.domain.Account;
import com.example.carmarketplace.security.AccountPrincipal;
import com.example.carmarketplace.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AccountService accountService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final CsrfTokenRepository csrfTokenRepository;

    public AuthController(AccountService accountService,
                          AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository,
                          CsrfTokenRepository csrfTokenRepository) {
        this.accountService = accountService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(@RequestAttribute("_csrf") CsrfToken csrfToken) {
        return Map.of("token", csrfToken.getToken(), "headerName", csrfToken.getHeaderName());
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponses.AccountView> register(
            @Valid @RequestBody ApiRequests.RegisterRequest request) {
        Account account = accountService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponses.account(account));
    }

    @PostMapping("/login")
    public ApiResponses.AccountView login(
            @Valid @RequestBody ApiRequests.LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));

        HttpSession session = servletRequest.getSession(true);
        servletRequest.changeSessionId();
        csrfTokenRepository.saveToken(null, servletRequest, servletResponse);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);
        return ApiResponses.account(((AccountPrincipal) authentication.getPrincipal()).getAccount());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        csrfTokenRepository.saveToken(null, servletRequest, servletResponse);
        HttpSession session = servletRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }
}
