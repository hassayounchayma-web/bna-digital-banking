package com.bna.digitalbanking.simulation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreditMarginResponse(
    Long id,
    CreditScenario scenario,
    String creditType,
    BigDecimal marginPercent,
    Integer maxMonths,
    LocalDateTime modifiedAt
) {
    static CreditMarginResponse from(CreditMarginEntity entity) {
        return new CreditMarginResponse(
            entity.getId(),
            entity.getScenario(),
            entity.getCreditType(),
            entity.getMarginPercent(),
            entity.getMaxMonths(),
            entity.getModifiedAt());
    }
}
