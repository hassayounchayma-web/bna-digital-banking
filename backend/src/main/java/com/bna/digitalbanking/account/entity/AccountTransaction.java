package com.bna.digitalbanking.account.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
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

@Entity
@Table(name = "account_transactions")
public class AccountTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36, updatable = false)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private BankAccount account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountTransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private TransactionCategory category;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "occurred_at", nullable = false)
    private LocalDate occurredAt;

    @Column(length = 200)
    private String description;

    @Column(name = "transfer_reference", length = 36)
    private String transferReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AccountTransaction() {
    }

    public AccountTransaction(
            BankAccount account,
            AccountTransactionType type,
            TransactionCategory category,
            BigDecimal amount,
            LocalDate occurredAt,
            String description,
            String transferReference) {
        this.account = account;
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.occurredAt = occurredAt;
        this.description = description;
        this.transferReference = transferReference;
    }

    @PrePersist
    void onCreate() {
        reference = UUID.randomUUID().toString();
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public BankAccount getAccount() { return account; }
    public AccountTransactionType getType() { return type; }
    public TransactionCategory getCategory() { return category; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getOccurredAt() { return occurredAt; }
    public String getDescription() { return description; }
    public String getTransferReference() { return transferReference; }
    public Instant getCreatedAt() { return createdAt; }
}
