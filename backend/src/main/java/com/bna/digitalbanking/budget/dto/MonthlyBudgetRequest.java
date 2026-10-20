package com.bna.digitalbanking.budget.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import com.bna.digitalbanking.account.entity.TransactionCategory;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MonthlyBudgetRequest(
    @NotNull YearMonth month,
    @NotNull TransactionCategory category,
    @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal monthlyLimit
) {
}
