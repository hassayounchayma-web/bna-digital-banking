package com.bna.digitalbanking.account.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.bna.digitalbanking.identity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "bank_accounts", uniqueConstraints = {
    @UniqueConstraint(name = "uk_bank_account_customer_type", columnNames = {"customer_id", "account_type"}),
    @UniqueConstraint(name = "uk_bank_account_reference", columnNames = "reference")
})
public class BankAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36, updatable = false)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private UserEntity customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType type;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "opening_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal openingBalance;

    @Column(name = "current_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentBalance;

    @Column(nullable = false, length = 3)
    private String currency = "TND";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BankAccount() {
    }

    public BankAccount(UserEntity customer, AccountType type, String name, BigDecimal openingBalance) {
        this.customer = customer;
        this.type = type;
        this.name = name;
        this.openingBalance = openingBalance;
        this.currentBalance = openingBalance;
    }

    public void applyIncome(BigDecimal amount) {
        currentBalance = currentBalance.add(amount);
    }

    public void applyExpense(BigDecimal amount) {
        currentBalance = currentBalance.subtract(amount);
    }

    @PrePersist
    void onCreate() {
        reference = UUID.randomUUID().toString();
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public UserEntity getCustomer() { return customer; }
    public AccountType getType() { return type; }
    public String getName() { return name; }
    public BigDecimal getOpeningBalance() { return openingBalance; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public String getCurrency() { return currency; }
    public Instant getCreatedAt() { return createdAt; }
}
