package com.bna.digitalbanking.credit.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import com.bna.digitalbanking.credit.application.entity.ApplicationEvent;
import com.bna.digitalbanking.credit.application.entity.ApplicationStatus;
import com.bna.digitalbanking.credit.application.entity.CreditApplication;
import com.bna.digitalbanking.credit.ai.dto.AiEvaluationResponse;
import com.bna.digitalbanking.credit.ai.dto.EvaluationComparisonResponse;
import com.bna.digitalbanking.credit.scoring.dto.CreditScoringResponse;
import com.bna.digitalbanking.simulation.CreditScenario;

public record ApplicationResponse(
    Long id,
    String reference,
    String customerName,
    String customerEmail,
    CreditScenario scenario,
    String creditType,
    BigDecimal requestedAmount,
    Integer durationMonths,
    BigDecimal averageMonthlySalary,
    BigDecimal halfRentIncome,
    BigDecimal sedDeductions,
    BigDecimal bnaDeductions,
    BigDecimal bnaNonBuyoutDeductions,
    BigDecimal remainingBuyoutCapital,
    ApplicationStatus status,
    Instant createdAt,
    Instant updatedAt,
    Instant submittedAt,
    Integer incomeStabilityMonths,
    Integer latePaymentsLast12Months,
    Integer savingsRegularityMonthsLast12Months,
    List<ApplicationEventResponse> history,
    SimulationSnapshotResponse simulation,
    CreditScoringResponse scoring,
    AiEvaluationResponse aiEvaluation,
    EvaluationComparisonResponse comparison
) {
    public static ApplicationResponse from(
            CreditApplication application,
            List<ApplicationEvent> events,
            CreditScoringResponse scoring,
            AiEvaluationResponse aiEvaluation,
            EvaluationComparisonResponse comparison) {
        return new ApplicationResponse(
            application.getId(),
            application.getReference(),
            application.getCustomer().getFullName(),
            application.getCustomer().getEmail(),
            application.getScenario(),
            application.getCreditType(),
            application.getRequestedAmount(),
            application.getDurationMonths(),
            application.getAverageMonthlySalary(),
            application.getHalfRentIncome(),
            application.getSedDeductions(),
            application.getBnaDeductions(),
            application.getBnaNonBuyoutDeductions(),
            application.getRemainingBuyoutCapital(),
            application.getStatus(),
            application.getCreatedAt(),
            application.getUpdatedAt(),
            application.getSubmittedAt(),
            application.getIncomeStabilityMonths(),
            application.getLatePaymentsLast12Months(),
            application.getSavingsRegularityMonthsLast12Months(),
            events.stream().map(ApplicationEventResponse::from).toList(),
            SimulationSnapshotResponse.from(application.getSimulationSnapshot()),
            scoring,
            aiEvaluation,
            comparison);
    }
}
