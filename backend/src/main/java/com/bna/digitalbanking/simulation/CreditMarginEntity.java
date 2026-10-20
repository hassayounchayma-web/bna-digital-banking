package com.bna.digitalbanking.simulation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "credit_margin",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_credit_margin_scenario_product",
        columnNames = {"scenario", "credit_type"}))
public class CreditMarginEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CreditScenario scenario;

    @Column(name = "credit_type", nullable = false, length = 255)
    private String creditType;

    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal marginPercent;

    @Column(nullable = false)
    private Integer maxMonths;

    @Column(nullable = false)
    private LocalDateTime modifiedAt;

    protected CreditMarginEntity() {
    }

    public CreditMarginEntity(
            CreditScenario scenario,
            String creditType,
            BigDecimal marginPercent,
            Integer maxMonths) {
        this.scenario = scenario;
        this.creditType = creditType;
        this.marginPercent = marginPercent;
        this.maxMonths = maxMonths;
    }

    @PrePersist
    @PreUpdate
    public void updateModifiedAt() {
        modifiedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public CreditScenario getScenario() {
        return scenario;
    }

    public String getCreditType() {
        return creditType;
    }

    public BigDecimal getMarginPercent() {
        return marginPercent;
    }

    public Integer getMaxMonths() {
        return maxMonths;
    }

    public LocalDateTime getModifiedAt() {
        return modifiedAt;
    }

    public void update(BigDecimal marginPercent, Integer maxMonths) {
        this.marginPercent = marginPercent;
        this.maxMonths = maxMonths;
    }
}
