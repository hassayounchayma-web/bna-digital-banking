package com.bna.digitalbanking.credit.scoring.dto;

import java.math.BigDecimal;
import com.bna.digitalbanking.credit.scoring.entity.ScoringFactor;

public record ScoringFactorResponse(
    String code,
    String label,
    BigDecimal points,
    BigDecimal maximumPoints,
    String inputValue,
    String explanation,
    boolean available
) {
    public static ScoringFactorResponse from(ScoringFactor factor) {
        return new ScoringFactorResponse(
            factor.getCode(),
            factor.getLabel(),
            factor.getPoints(),
            factor.getMaximumPoints(),
            factor.getInputValue(),
            factor.getExplanation(),
            factor.isAvailable());
    }
}
