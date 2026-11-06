package com.example.carmarketplace.domain;

public class AdminAccount extends Account {
    public AdminAccount() {
    }

    public AdminAccount(String id, String email, String passwordHash, String fullName, String phone) {
        super(id, email, passwordHash, fullName, phone);
    }

    @Override
    public AccountRole getRole() {
        return AccountRole.ADMIN;
    }
}
