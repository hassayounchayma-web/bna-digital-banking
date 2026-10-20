package com.bna.digitalbanking.credit.scoring.entity;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ScoringFactor {
    @Column(name = "factor_code", nullable = false, length = 40)
    private String code;

    @Column(name = "factor_label", nullable = false, length = 120)
    private String label;

    @Column(name = "factor_points", precision = 6, scale = 2)
    private BigDecimal points;

    @Column(name = "factor_max_points", nullable = false, precision = 6, scale = 2)
    private BigDecimal maximumPoints;

    @Column(name = "factor_input", length = 180)
    private String inputValue;

    @Column(name = "factor_explanation", nullable = false, length = 500)
    private String explanation;

    protected ScoringFactor() {
    }

    public ScoringFactor(
            String code,
            String label,
            BigDecimal points,
            BigDecimal maximumPoints,
            String inputValue,
            String explanation) {
        this.code = code;
        this.label = label;
        this.points = points;
        this.maximumPoints = maximumPoints;
        this.inputValue = inputValue;
        this.explanation = explanation;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }
    public BigDecimal getPoints() { return points; }
    public BigDecimal getMaximumPoints() { return maximumPoints; }
    public String getInputValue() { return inputValue; }
    public String getExplanation() { return explanation; }
    public boolean isAvailable() { return points != null; }
}
