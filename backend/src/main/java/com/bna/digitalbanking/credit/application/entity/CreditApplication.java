package com.bna.digitalbanking.credit.application.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.bna.digitalbanking.credit.application.dto.ApplicationRequest;
import com.bna.digitalbanking.identity.UserEntity;
import com.bna.digitalbanking.simulation.CreditScenario;
import com.bna.digitalbanking.simulation.SimulationResult;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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

@Entity
@Table(name = "credit_applications")
public class CreditApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36, updatable = false)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private UserEntity customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CreditScenario scenario;

    @Column(name = "credit_type", nullable = false, length = 255)
    private String creditType;

    @Column(name = "requested_amount", precision = 15, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths;

    @Column(name = "average_monthly_salary", nullable = false, precision = 15, scale = 2)
    private BigDecimal averageMonthlySalary;

    @Column(name = "half_rent_income", nullable = false, precision = 15, scale = 2)
    private BigDecimal halfRentIncome;

    @Column(name = "sed_deductions", nullable = false, precision = 15, scale = 2)
    private BigDecimal sedDeductions;

    @Column(name = "bna_deductions", nullable = false, precision = 15, scale = 2)
    private BigDecimal bnaDeductions;

    @Column(name = "bna_non_buyout_deductions", nullable = false, precision = 15, scale = 2)
    private BigDecimal bnaNonBuyoutDeductions;

    @Column(name = "remaining_buyout_capital", nullable = false, precision = 15, scale = 2)
    private BigDecimal remainingBuyoutCapital;

    @Column(name = "income_stability_months")
    private Integer incomeStabilityMonths;

    @Column(name = "savings_regular_months_last_12")
    private Integer savingsRegularityMonthsLast12Months;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Embedded
    private CreditSimulationSnapshot simulationSnapshot;

    protected CreditApplication() {
    }

    public CreditApplication(ApplicationRequest request, UserEntity customer, SimulationResult simulationResult) {
        this.customer = customer;
        this.status = ApplicationStatus.DRAFT;
        updateDetails(request);
        this.simulationSnapshot = CreditSimulationSnapshot.from(simulationResult);
    }

    public void updateDetails(ApplicationRequest request) {
        this.scenario = request.scenario();
        this.creditType = request.creditType().strip();
        this.requestedAmount = request.requestedAmount();
        this.durationMonths = request.durationMonths();
        this.averageMonthlySalary = request.averageMonthlySalary();
        this.halfRentIncome = request.halfRentIncome();
        this.sedDeductions = request.sedDeductions();
        this.bnaDeductions = request.bnaDeductions();
        this.bnaNonBuyoutDeductions = request.bnaNonBuyoutDeductions();
        this.remainingBuyoutCapital = request.remainingBuyoutCapital();
    }

    public void updateScoringEvidence(
            Integer incomeStabilityMonths,
            Integer savingsRegularityMonthsLast12Months) {
        this.incomeStabilityMonths = incomeStabilityMonths;
        this.savingsRegularityMonthsLast12Months = savingsRegularityMonthsLast12Months;
    }

    public void updateSimulationSnapshot(SimulationResult simulationResult) {
        this.simulationSnapshot = CreditSimulationSnapshot.from(simulationResult);
    }

    public void transition(ApplicationStatus status) {
        this.status = status;
        if (status == ApplicationStatus.SUBMITTED && submittedAt == null) {
            submittedAt = Instant.now();
        }
    }

    @PrePersist
    void onCreate() {
        reference = UUID.randomUUID().toString();
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public UserEntity getCustomer() { return customer; }
    public CreditScenario getScenario() { return scenario; }
    public String getCreditType() { return creditType; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public Integer getDurationMonths() { return durationMonths; }
    public BigDecimal getAverageMonthlySalary() { return averageMonthlySalary; }
    public BigDecimal getHalfRentIncome() { return halfRentIncome; }
    public BigDecimal getSedDeductions() { return sedDeductions; }
    public BigDecimal getBnaDeductions() { return bnaDeductions; }
    public BigDecimal getBnaNonBuyoutDeductions() { return bnaNonBuyoutDeductions; }
    public BigDecimal getRemainingBuyoutCapital() { return remainingBuyoutCapital; }
    public Integer getIncomeStabilityMonths() { return incomeStabilityMonths; }
    public Integer getSavingsRegularityMonthsLast12Months() { return savingsRegularityMonthsLast12Months; }
    public ApplicationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public CreditSimulationSnapshot getSimulationSnapshot() { return simulationSnapshot; }
}
