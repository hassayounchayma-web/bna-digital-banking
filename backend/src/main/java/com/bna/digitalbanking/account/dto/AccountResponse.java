package com.bna.digitalbanking.account.dto;

import java.math.BigDecimal;
import java.time.Instant;
import com.bna.digitalbanking.account.entity.BankAccount;

public record AccountResponse(
    Long id,
    String reference,
    String type,
    String name,
    BigDecimal openingBalance,
    BigDecimal currentBalance,
    String currency,
    Instant createdAt
) {
    public static AccountResponse from(BankAccount account) {
        return new AccountResponse(
            account.getId(),
            account.getReference(),
            account.getType().name(),
            account.getName(),
            account.getOpeningBalance(),
            account.getCurrentBalance(),
            account.getCurrency(),
            account.getCreatedAt());
    }
}
