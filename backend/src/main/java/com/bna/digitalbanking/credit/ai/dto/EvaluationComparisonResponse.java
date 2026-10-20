package com.bna.digitalbanking.credit.ai.dto;

import java.time.Instant;
import java.util.List;
import com.bna.digitalbanking.credit.ai.entity.EvaluationComparison;

public record EvaluationComparisonResponse(
    String javaEligibilityStatus,
    String aiEligibilityStatus,
    Boolean eligibilityAgreement,
    String javaRiskLevel,
    String aiRiskLevel,
    Boolean riskAgreement,
    boolean scoringProvisional,
    List<String> explanations,
    Instant comparedAt
) {
    public static EvaluationComparisonResponse from(EvaluationComparison comparison) {
        return new EvaluationComparisonResponse(
            comparison.getJavaEligibilityStatus(),
            comparison.getAiEligibilityStatus().name(),
            comparison.getEligibilityAgreement(),
            comparison.getJavaRiskLevel(),
            comparison.getAiRiskLevel().name(),
            comparison.getRiskAgreement(),
            comparison.isScoringProvisional(),
            comparison.getExplanations(),
            comparison.getComparedAt());
    }
}
