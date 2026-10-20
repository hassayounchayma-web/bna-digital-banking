package com.bna.digitalbanking.credit.ai.dto;

import java.time.Instant;
import java.util.List;
import com.bna.digitalbanking.credit.ai.entity.AiEvaluation;

public record AiEvaluationResponse(
    String status,
    String eligibilityStatus,
    String riskLevel,
    List<String> reasons,
    List<String> missingInformation,
    List<String> warnings,
    String message,
    Instant evaluatedAt
) {
    public static AiEvaluationResponse from(AiEvaluation evaluation) {
        return new AiEvaluationResponse(
            evaluation.getStatus().name(),
            evaluation.getEligibilityStatus().name(),
            evaluation.getRiskLevel().name(),
            evaluation.getReasons(),
            evaluation.getMissingInformation(),
            evaluation.getWarnings(),
            evaluation.getMessage(),
            evaluation.getEvaluatedAt());
    }
}
