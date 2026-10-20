package com.bna.digitalbanking.budget.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record BudgetResponse(
    YearMonth month,
    BigDecimal totalIncome,
    BigDecimal totalExpenses,
    List<BudgetCategoryResponse> categories
) {
}
