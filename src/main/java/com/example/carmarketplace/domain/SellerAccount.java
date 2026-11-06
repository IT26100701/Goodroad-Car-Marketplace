package com.example.carmarketplace.domain;

public class SellerAccount extends Account {
    public SellerAccount() {
    }

    public SellerAccount(String id, String email, String passwordHash, String fullName, String phone) {
        super(id, email, passwordHash, fullName, phone);
    }

    @Override
    public AccountRole getRole() {
        return AccountRole.SELLER;
    }
}
