package com.bna.digitalbanking.credit.application.dto;

import java.math.BigDecimal;
import com.bna.digitalbanking.credit.application.entity.CreditSimulationSnapshot;

public record SimulationSnapshotResponse(
    BigDecimal netIncome,
    BigDecimal debtRatePercent,
    BigDecimal safeguardPercent,
    BigDecimal monthlyCapacity,
    BigDecimal annualRatePercent,
    BigDecimal maximumAmount,
    BigDecimal monthlyPayment,
    String status,
    String message
) {
    public static SimulationSnapshotResponse from(CreditSimulationSnapshot snapshot) {
        if (snapshot == null) return null;
        return new SimulationSnapshotResponse(
            snapshot.getNetIncome(),
            snapshot.getDebtRatePercent(),
            snapshot.getSafeguardPercent(),
            snapshot.getMonthlyCapacity(),
            snapshot.getAnnualRatePercent(),
            snapshot.getMaximumAmount(),
            snapshot.getMonthlyPayment(),
            snapshot.getStatus(),
            snapshot.getMessage());
    }
}
