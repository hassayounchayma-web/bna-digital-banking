package com.bna.digitalbanking.credit.ai.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import com.bna.digitalbanking.credit.application.entity.CreditApplication;
import com.bna.digitalbanking.credit.ai.dto.AiAssessmentPayload;
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
@Table(name = "credit_ai_evaluations")
public class AiEvaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private CreditApplication application;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private AiEvaluationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "eligibility_status", nullable = false, length = 24)
    private AiEligibilityStatus eligibilityStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private AiRiskLevel riskLevel;

    @Column(length = 500)
    private String message;

    @Column(name = "evaluated_by", nullable = false, length = 254)
    private String evaluatedBy;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    @ElementCollection
    @CollectionTable(name = "credit_ai_reasons", joinColumns = @JoinColumn(name = "evaluation_id"))
    @OrderColumn(name = "reason_order")
    @Column(name = "reason_text", nullable = false, length = 500)
    private List<String> reasons = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "credit_ai_missing_information", joinColumns = @JoinColumn(name = "evaluation_id"))
    @OrderColumn(name = "information_order")
    @Column(name = "information_text", nullable = false, length = 500)
    private List<String> missingInformation = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "credit_ai_warnings", joinColumns = @JoinColumn(name = "evaluation_id"))
    @OrderColumn(name = "warning_order")
    @Column(name = "warning_text", nullable = false, length = 500)
    private List<String> warnings = new ArrayList<>();

    protected AiEvaluation() {
    }

    public AiEvaluation(CreditApplication application) {
        this.application = application;
    }

    public void update(
            AiEvaluationStatus newStatus,
            AiEligibilityStatus newEligibilityStatus,
            AiRiskLevel newRiskLevel,
            List<String> newReasons,
            List<String> newMissingInformation,
            List<String> newWarnings,
            String newMessage,
            String evaluator) {
        status = newStatus;
        eligibilityStatus = newEligibilityStatus;
        riskLevel = newRiskLevel;
        reasons.clear();
        reasons.addAll(newReasons);
        missingInformation.clear();
        missingInformation.addAll(newMissingInformation);
        warnings.clear();
        warnings.addAll(newWarnings);
        message = newMessage;
        evaluatedBy = evaluator;
        evaluatedAt = Instant.now();
    }

    public AiEvaluationStatus getStatus() { return status; }
    public AiEligibilityStatus getEligibilityStatus() { return eligibilityStatus; }
    public AiRiskLevel getRiskLevel() { return riskLevel; }
    public List<String> getReasons() { return List.copyOf(reasons); }
    public List<String> getMissingInformation() { return List.copyOf(missingInformation); }
    public List<String> getWarnings() { return List.copyOf(warnings); }
    public String getMessage() { return message; }
    public String getEvaluatedBy() { return evaluatedBy; }
    public Instant getEvaluatedAt() { return evaluatedAt; }
}
