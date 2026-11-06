package com.example.carmarketplace.domain;

public class BuyerAccount extends Account {
    public BuyerAccount() {
    }

    public BuyerAccount(String id, String email, String passwordHash, String fullName, String phone) {
        super(id, email, passwordHash, fullName, phone);
    }

    @Override
    public AccountRole getRole() {
        return AccountRole.BUYER;
    }
}
