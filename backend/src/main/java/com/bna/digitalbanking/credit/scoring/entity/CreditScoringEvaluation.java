package com.bna.digitalbanking.credit.scoring.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import com.bna.digitalbanking.credit.application.entity.CreditApplication;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "credit_scoring_evaluations")
public class CreditScoringEvaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private CreditApplication application;

    @Column(name = "score_out_of_100", nullable = false, precision = 6, scale = 2)
    private BigDecimal scoreOutOf100;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private CreditRiskLevel riskLevel;

    @Column(nullable = false)
    private boolean provisional;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    @ElementCollection
    @CollectionTable(
        name = "credit_scoring_factors",
        joinColumns = @JoinColumn(name = "evaluation_id"))
    @OrderColumn(name = "factor_order")
    private List<ScoringFactor> factors = new ArrayList<>();

    protected CreditScoringEvaluation() {
    }

    public CreditScoringEvaluation(
            CreditApplication application,
            BigDecimal scoreOutOf100,
            CreditRiskLevel riskLevel,
            boolean provisional,
            List<ScoringFactor> factors) {
        this.application = application;
        this.scoreOutOf100 = scoreOutOf100;
        this.riskLevel = riskLevel;
        this.provisional = provisional;
        this.factors.addAll(factors);
        this.evaluatedAt = Instant.now();
    }

    public void refreshFrom(CreditScoringEvaluation refreshed) {
        scoreOutOf100 = refreshed.scoreOutOf100;
        riskLevel = refreshed.riskLevel;
        provisional = refreshed.provisional;
        factors.clear();
        factors.addAll(refreshed.factors);
        evaluatedAt = refreshed.evaluatedAt;
    }

    public CreditApplication getApplication() { return application; }
    public BigDecimal getScoreOutOf100() { return scoreOutOf100; }
    public CreditRiskLevel getRiskLevel() { return riskLevel; }
    public boolean isProvisional() { return provisional; }
    public Instant getEvaluatedAt() { return evaluatedAt; }
    public List<ScoringFactor> getFactors() { return List.copyOf(factors); }
}
