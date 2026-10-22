package com.bna.digitalbanking.credit.ai.service;

import java.util.List;
import com.bna.digitalbanking.credit.ai.dto.CreditAiFacts;
import com.bna.digitalbanking.credit.ai.entity.AiEligibilityStatus;
import com.bna.digitalbanking.credit.ai.entity.AiEvaluationStatus;
import com.bna.digitalbanking.credit.ai.entity.AiRiskLevel;
import com.bna.digitalbanking.credit.application.entity.CreditApplication;
import com.bna.digitalbanking.credit.application.entity.CreditSimulationSnapshot;
import com.bna.digitalbanking.credit.application.repository.CreditApplicationRepository;
import com.bna.digitalbanking.credit.ai.service.CreditAiEvaluationPersistenceService.AiComparisonResult;
import com.bna.digitalbanking.credit.ai.service.CreditAiEvaluationPersistenceService.AiEvaluationStatusResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CreditAiEvaluationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreditAiEvaluationService.class);
    private static final String NOT_CONFIGURED =
        "L’analyse IA n’est pas configurée. Définissez LLM_ENABLED, LLM_MODEL et LLM_API_KEY dans l’environnement du backend.";
    private static final String UNAVAILABLE =
        "Le service IA est indisponible. Le scoring Java reste disponible et indépendant.";
    private static final String INVALID_RESPONSE =
        "La réponse du service IA ne respecte pas le contrat JSON attendu.";

    private final CreditApplicationRepository applications;
    private final CreditAiProvider provider;
    private final AiAssessmentParser parser;
    private final CreditAiEvaluationPersistenceService persistence;

    public CreditAiEvaluationService(
            CreditApplicationRepository applications,
            CreditAiProvider provider,
            AiAssessmentParser parser,
            CreditAiEvaluationPersistenceService persistence) {
        this.applications = applications;
        this.provider = provider;
        this.parser = parser;
        this.persistence = persistence;
    }

    public AiComparisonResult evaluate(Long applicationId, String evaluator) {
        CreditApplication application = applications.findById(applicationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable."));
        CreditSimulationSnapshot snapshot = application.getSimulationSnapshot();
        AiEvaluationStatusResult result;
        if (!provider.isConfigured()) {
            result = unavailable(NOT_CONFIGURED);
        } else {
            try {
                AiAssessmentParser.ParsedAiAssessment parsed = parser.parse(provider.analyze(toFacts(application, snapshot)));
                result = new AiEvaluationStatusResult(
                    AiEvaluationStatus.SUCCESS,
                    parsed.eligibilityStatus(),
                    parsed.riskLevel(),
                    parsed.reasons(),
                    parsed.missingInformation(),
                    parsed.warnings(),
                    null);
            } catch (CreditAiProviderException exception) {
                LOGGER.warn("IA indisponible pour la demande {} : {}", applicationId, exception.getMessage());
                result = unavailable(UNAVAILABLE + " Diagnostic fournisseur : " + exception.getMessage());
            } catch (IllegalArgumentException exception) {
                result = new AiEvaluationStatusResult(
                    AiEvaluationStatus.INVALID_RESPONSE,
                    AiEligibilityStatus.INCONCLUSIVE,
                    AiRiskLevel.UNKNOWN,
                    List.of(),
                    List.of(),
                    List.of(),
                    INVALID_RESPONSE);
            }
        }
        return persistence.persist(applicationId, evaluator, result);
    }

    private CreditAiFacts toFacts(CreditApplication application, CreditSimulationSnapshot snapshot) {
        if (snapshot == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L’instantané de simulation est introuvable.");
        }
        return new CreditAiFacts(
            application.getScenario().name(),
            application.getRequestedAmount(),
            application.getDurationMonths(),
            snapshot.getNetIncome(),
            snapshot.getCalculatedDebtRatePercent(),
            snapshot.getMonthlyCapacity(),
            snapshot.getMonthlyPayment(),
            application.getRemainingBuyoutCapital(),
            application.getIncomeStabilityMonths(),
            application.getSavingsRegularityMonthsLast12Months());
    }

    private AiEvaluationStatusResult unavailable(String message) {
        return new AiEvaluationStatusResult(
            AiEvaluationStatus.UNAVAILABLE,
            AiEligibilityStatus.INCONCLUSIVE,
            AiRiskLevel.UNKNOWN,
            List.of(),
            List.of(),
            List.of(),
            message);
    }
}
