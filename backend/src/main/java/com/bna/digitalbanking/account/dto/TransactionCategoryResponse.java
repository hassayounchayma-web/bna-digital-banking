package com.bna.digitalbanking.account.dto;

import com.bna.digitalbanking.account.entity.TransactionCategory;

public record TransactionCategoryResponse(
    String code,
    String label,
    boolean budgetedExpense
) {
    public static TransactionCategoryResponse from(TransactionCategory category) {
        return new TransactionCategoryResponse(
            category.name(),
            category.getLabel(),
            category.isBudgetedExpense());
    }
}
