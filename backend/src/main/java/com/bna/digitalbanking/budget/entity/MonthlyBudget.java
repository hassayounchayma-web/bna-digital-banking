package com.bna.digitalbanking.budget.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import com.bna.digitalbanking.account.entity.TransactionCategory;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "monthly_budgets", uniqueConstraints = {
    @UniqueConstraint(
        name = "uk_monthly_budget_customer_month_category",
        columnNames = {"customer_id", "budget_month", "category"})
})
public class MonthlyBudget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private UserEntity customer;

    @Column(name = "budget_month", nullable = false)
    private LocalDate month;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionCategory category;

    @Column(name = "monthly_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyLimit;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MonthlyBudget() {
    }

    public MonthlyBudget(
            UserEntity customer,
            LocalDate month,
            TransactionCategory category,
            BigDecimal monthlyLimit) {
        this.customer = customer;
        this.month = month;
        this.category = category;
        this.monthlyLimit = monthlyLimit;
    }

    public void updateLimit(BigDecimal newLimit) {
        monthlyLimit = newLimit;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public UserEntity getCustomer() { return customer; }
    public LocalDate getMonth() { return month; }
    public TransactionCategory getCategory() { return category; }
    public BigDecimal getMonthlyLimit() { return monthlyLimit; }
}
