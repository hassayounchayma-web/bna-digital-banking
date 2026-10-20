package com.bna.digitalbanking.budget.dto;

import java.math.BigDecimal;

public record BudgetCategoryResponse(
    String category,
    String label,
    BigDecimal monthlyLimit,
    BigDecimal spent,
    BigDecimal remaining,
    boolean exceeded
) {
}
