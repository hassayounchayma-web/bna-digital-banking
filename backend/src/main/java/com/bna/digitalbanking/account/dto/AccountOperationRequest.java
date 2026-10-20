package com.bna.digitalbanking.account.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.bna.digitalbanking.account.entity.TransactionCategory;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AccountOperationRequest(
    @NotNull OperationType type,
    @NotNull TransactionCategory category,
    @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal amount,
    @NotNull @PastOrPresent LocalDate occurredAt,
    @Size(max = 200) String description
) {
    public enum OperationType {
        INCOME,
        EXPENSE
    }
}
