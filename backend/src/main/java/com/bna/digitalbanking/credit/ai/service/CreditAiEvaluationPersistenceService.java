package com.bna.digitalbanking.credit.ai.service;

import com.bna.digitalbanking.credit.ai.dto.AiEvaluationResponse;
import com.bna.digitalbanking.credit.ai.dto.EvaluationComparisonResponse;
import com.bna.digitalbanking.credit.ai.entity.AiEvaluation;
import com.bna.digitalbanking.credit.ai.entity.EvaluationComparison;
import com.bna.digitalbanking.credit.ai.repository.AiEvaluationRepository;
import com.bna.digitalbanking.credit.ai.repository.EvaluationComparisonRepository;
import com.bna.digitalbanking.credit.application.entity.CreditApplication;
import com.bna.digitalbanking.credit.application.repository.CreditApplicationRepository;
import com.bna.digitalbanking.credit.scoring.entity.CreditScoringEvaluation;
import com.bna.digitalbanking.credit.scoring.repository.CreditScoringEvaluationRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CreditAiEvaluationPersistenceService {
    private final CreditApplicationRepository applications;
    private final CreditScoringEvaluationRepository scoringEvaluations;
    private final AiEvaluationRepository aiEvaluations;
    private final EvaluationComparisonRepository comparisons;
    private final EvaluationComparisonService comparisonService;

    public CreditAiEvaluationPersistenceService(
            CreditApplicationRepository applications,
            CreditScoringEvaluationRepository scoringEvaluations,
            AiEvaluationRepository aiEvaluations,
            EvaluationComparisonRepository comparisons,
            EvaluationComparisonService comparisonService) {
        this.applications = applications;
        this.scoringEvaluations = scoringEvaluations;
        this.aiEvaluations = aiEvaluations;
        this.comparisons = comparisons;
        this.comparisonService = comparisonService;
    }

    @Transactional
    public AiComparisonResult persist(
            Long applicationId,
            String evaluator,
            AiEvaluationStatusResult result) {
        CreditApplication application = applications.findById(applicationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable."));
        CreditScoringEvaluation scoring = scoringEvaluations.findByApplicationId(applicationId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Le score Java n’est pas disponible pour cette demande."));
        AiEvaluation evaluation = aiEvaluations.findByApplicationId(applicationId)
            .orElseGet(() -> new AiEvaluation(application));
        evaluation.update(
            result.status(),
            result.eligibilityStatus(),
            result.riskLevel(),
            result.reasons(),
            result.missingInformation(),
            result.warnings(),
            result.message(),
            evaluator);
        AiEvaluation savedEvaluation = aiEvaluations.save(evaluation);
        EvaluationComparison comparison = comparisons.findByApplicationId(applicationId).orElse(null);
        EvaluationComparison savedComparison = comparisons.save(
            comparisonService.compare(application, scoring, savedEvaluation, comparison));
        return new AiComparisonResult(
            AiEvaluationResponse.from(savedEvaluation),
            EvaluationComparisonResponse.from(savedComparison));
    }

    public record AiEvaluationStatusResult(
        com.bna.digitalbanking.credit.ai.entity.AiEvaluationStatus status,
        com.bna.digitalbanking.credit.ai.entity.AiEligibilityStatus eligibilityStatus,
        com.bna.digitalbanking.credit.ai.entity.AiRiskLevel riskLevel,
        java.util.List<String> reasons,
        java.util.List<String> missingInformation,
        java.util.List<String> warnings,
        String message
    ) {
    }

    public record AiComparisonResult(
        AiEvaluationResponse aiEvaluation,
        EvaluationComparisonResponse comparison
    ) {
    }
}
