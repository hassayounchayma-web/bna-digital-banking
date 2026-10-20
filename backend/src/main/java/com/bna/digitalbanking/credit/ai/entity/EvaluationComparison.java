package com.bna.digitalbanking.credit.ai.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import com.bna.digitalbanking.credit.application.entity.CreditApplication;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "credit_evaluation_comparisons")
public class EvaluationComparison {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private CreditApplication application;

    @Column(name = "java_eligibility_status", nullable = false, length = 24)
    private String javaEligibilityStatus;

    @Column(name = "ai_eligibility_status", nullable = false, length = 24)
    private AiEligibilityStatus aiEligibilityStatus;

    @Column(name = "eligibility_agreement")
    private Boolean eligibilityAgreement;

    @Column(name = "java_risk_level", nullable = false, length = 20)
    private String javaRiskLevel;

    @Column(name = "ai_risk_level", nullable = false, length = 20)
    private AiRiskLevel aiRiskLevel;

    @Column(name = "risk_agreement")
    private Boolean riskAgreement;

    @Column(name = "scoring_provisional", nullable = false)
    private boolean scoringProvisional;

    @Column(name = "compared_at", nullable = false)
    private Instant comparedAt;

    @ElementCollection
    @CollectionTable(name = "credit_comparison_explanations", joinColumns = @JoinColumn(name = "comparison_id"))
    @OrderColumn(name = "explanation_order")
    @Column(name = "explanation_text", nullable = false, length = 500)
    private List<String> explanations = new ArrayList<>();

    protected EvaluationComparison() {
    }

    public EvaluationComparison(CreditApplication application) {
        this.application = application;
    }

    public void update(
            String javaEligibility,
            AiEligibilityStatus aiEligibility,
            Boolean eligibilityMatch,
            String javaRisk,
            AiRiskLevel aiRisk,
            Boolean riskMatch,
            boolean provisional,
            List<String> details) {
        javaEligibilityStatus = javaEligibility;
        aiEligibilityStatus = aiEligibility;
        eligibilityAgreement = eligibilityMatch;
        javaRiskLevel = javaRisk;
        aiRiskLevel = aiRisk;
        riskAgreement = riskMatch;
        scoringProvisional = provisional;
        explanations.clear();
        explanations.addAll(details);
        comparedAt = Instant.now();
    }

    public String getJavaEligibilityStatus() { return javaEligibilityStatus; }
    public AiEligibilityStatus getAiEligibilityStatus() { return aiEligibilityStatus; }
    public Boolean getEligibilityAgreement() { return eligibilityAgreement; }
    public String getJavaRiskLevel() { return javaRiskLevel; }
    public AiRiskLevel getAiRiskLevel() { return aiRiskLevel; }
    public Boolean getRiskAgreement() { return riskAgreement; }
    public boolean isScoringProvisional() { return scoringProvisional; }
    public Instant getComparedAt() { return comparedAt; }
    public List<String> getExplanations() { return List.copyOf(explanations); }
}
