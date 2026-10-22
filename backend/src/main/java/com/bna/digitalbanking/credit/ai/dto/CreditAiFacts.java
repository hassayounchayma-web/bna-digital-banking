package com.bna.digitalbanking.credit.ai.dto;

import java.math.BigDecimal;

public record CreditAiFacts(
    String scenario,
    BigDecimal requestedAmount,
    Integer durationMonths,
    BigDecimal netIncome,
    BigDecimal calculatedDebtRatePercent,
    BigDecimal monthlyCapacity,
    BigDecimal requestedMonthlyPayment,
    BigDecimal remainingBuyoutCapital,
    Integer incomeStabilityMonthsObserved,
    Integer savingsRegularityMonthsLast12MonthsObserved
) {
}
