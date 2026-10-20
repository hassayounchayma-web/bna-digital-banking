package com.bna.digitalbanking.account.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import com.bna.digitalbanking.account.entity.AccountTransaction;

public record AccountTransactionResponse(
    Long id,
    String reference,
    Long accountId,
    String type,
    String category,
    String categoryLabel,
    BigDecimal amount,
    LocalDate occurredAt,
    String description,
    String transferReference,
    Instant createdAt
) {
    public static AccountTransactionResponse from(AccountTransaction transaction) {
        return new AccountTransactionResponse(
            transaction.getId(),
            transaction.getReference(),
            transaction.getAccount().getId(),
            transaction.getType().name(),
            transaction.getCategory() == null ? null : transaction.getCategory().name(),
            transaction.getCategory() == null ? null : transaction.getCategory().getLabel(),
            transaction.getAmount(),
            transaction.getOccurredAt(),
            transaction.getDescription(),
            transaction.getTransferReference(),
            transaction.getCreatedAt());
    }
}
