package com.bna.digitalbanking.credit.scoring.service;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

@Component
@Validated
@ConfigurationProperties(prefix = "app.credit-scoring")
public class CreditScoringProperties {
    @DecimalMin("0.0001")
    private BigDecimal availableIncomeFullScoreRatio = new BigDecimal("0.30");
    @Min(1)
    private int incomeStabilityFullScoreMonths = 24;
    @Min(1)
    private int savingsFullScoreMonths = 12;

    public BigDecimal getAvailableIncomeFullScoreRatio() { return availableIncomeFullScoreRatio; }
    public void setAvailableIncomeFullScoreRatio(BigDecimal value) { availableIncomeFullScoreRatio = value; }
    public int getIncomeStabilityFullScoreMonths() { return incomeStabilityFullScoreMonths; }
    public void setIncomeStabilityFullScoreMonths(int value) { incomeStabilityFullScoreMonths = value; }
    public int getSavingsFullScoreMonths() { return savingsFullScoreMonths; }
    public void setSavingsFullScoreMonths(int value) { savingsFullScoreMonths = value; }
}
