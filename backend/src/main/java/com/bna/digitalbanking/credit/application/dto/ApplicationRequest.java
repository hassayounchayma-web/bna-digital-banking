package com.bna.digitalbanking.credit.application.dto;

import java.math.BigDecimal;
import com.bna.digitalbanking.simulation.CreditScenario;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ApplicationRequest(
    @NotNull CreditScenario scenario,
    @NotBlank @Size(max = 255) String creditType,
    @Positive BigDecimal requestedAmount,
    @NotNull @Min(1) Integer durationMonths,
    @NotNull @Positive BigDecimal averageMonthlySalary,
    @NotNull @PositiveOrZero BigDecimal halfRentIncome,
    @NotNull @PositiveOrZero BigDecimal sedDeductions,
    @NotNull @PositiveOrZero BigDecimal bnaDeductions,
    @NotNull @PositiveOrZero BigDecimal bnaNonBuyoutDeductions,
    @NotNull @PositiveOrZero BigDecimal remainingBuyoutCapital
) {
}
