package com.bna.digitalbanking.credit.application.entity;

import java.math.BigDecimal;
import com.bna.digitalbanking.simulation.SimulationResult;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class CreditSimulationSnapshot {
    @Column(name = "sim_net_income", precision = 15, scale = 2)
    private BigDecimal netIncome;

    @Column(name = "sim_debt_rate", precision = 8, scale = 2)
    private BigDecimal debtRatePercent;

    @Column(name = "sim_safeguard", precision = 8, scale = 2)
    private BigDecimal safeguardPercent;

    @Column(name = "sim_monthly_capacity", precision = 15, scale = 2)
    private BigDecimal monthlyCapacity;

    @Column(name = "sim_annual_rate", precision = 8, scale = 4)
    private BigDecimal annualRatePercent;

    @Column(name = "sim_max_amount", precision = 15, scale = 2)
    private BigDecimal maximumAmount;

    @Column(name = "sim_monthly_payment", precision = 15, scale = 2)
    private BigDecimal monthlyPayment;

    @Column(name = "sim_calculated_debt_rate", precision = 8, scale = 2)
    private BigDecimal calculatedDebtRatePercent;

    @Column(name = "sim_status", length = 40)
    private String status;

    @Column(name = "sim_message", length = 500)
    private String message;

    protected CreditSimulationSnapshot() {
    }

    private CreditSimulationSnapshot(SimulationResult result) {
        this.netIncome = result.totalRevenuNetMoyen();
        this.debtRatePercent = result.tauxEndettement();
        this.safeguardPercent = result.gf();
        this.monthlyCapacity = result.availableMensualite();
        this.annualRatePercent = result.tauxAnnuel();
        this.maximumAmount = result.montantMaxCredit();
        this.monthlyPayment = result.mensualite();
        this.calculatedDebtRatePercent = result.tauxEndettementCalcule();
        this.status = result.statut();
        this.message = result.error();
    }

    public static CreditSimulationSnapshot from(SimulationResult result) {
        return new CreditSimulationSnapshot(result);
    }

    public BigDecimal getNetIncome() { return netIncome; }
    public BigDecimal getDebtRatePercent() { return debtRatePercent; }
    public BigDecimal getSafeguardPercent() { return safeguardPercent; }
    public BigDecimal getMonthlyCapacity() { return monthlyCapacity; }
    public BigDecimal getAnnualRatePercent() { return annualRatePercent; }
    public BigDecimal getMaximumAmount() { return maximumAmount; }
    public BigDecimal getMonthlyPayment() { return monthlyPayment; }
    public BigDecimal getCalculatedDebtRatePercent() { return calculatedDebtRatePercent; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
}
