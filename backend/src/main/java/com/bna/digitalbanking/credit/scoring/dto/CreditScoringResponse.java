package com.bna.digitalbanking.credit.scoring.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import com.bna.digitalbanking.credit.scoring.entity.CreditScoringEvaluation;

public record CreditScoringResponse(
    BigDecimal scoreOutOf100,
    String riskLevel,
    boolean provisional,
    Instant evaluatedAt,
    List<ScoringFactorResponse> factors,
    List<String> missingInformation,
    String disclaimer
) {
    private static final String DISCLAIMER =
        "Indicateur fictif de démonstration, non officiel et sans effet automatique sur la décision.";

    public static CreditScoringResponse from(CreditScoringEvaluation evaluation) {
        List<ScoringFactorResponse> factors = evaluation.getFactors().stream()
            .map(ScoringFactorResponse::from)
            .toList();
        return new CreditScoringResponse(
            evaluation.getScoreOutOf100(),
            evaluation.getRiskLevel().name(),
            evaluation.isProvisional(),
            evaluation.getEvaluatedAt(),
            factors,
            factors.stream().filter(factor -> !factor.available())
                .map(ScoringFactorResponse::label).toList(),
            DISCLAIMER);
    }
}
