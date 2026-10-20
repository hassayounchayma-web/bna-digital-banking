package com.bna.digitalbanking.credit.ai.service;

import java.util.ArrayList;
import java.util.List;
import com.bna.digitalbanking.credit.ai.entity.AiEligibilityStatus;
import com.bna.digitalbanking.credit.ai.entity.AiEvaluation;
import com.bna.digitalbanking.credit.ai.entity.AiEvaluationStatus;
import com.bna.digitalbanking.credit.ai.entity.AiRiskLevel;
import com.bna.digitalbanking.credit.ai.entity.EvaluationComparison;
import com.bna.digitalbanking.credit.application.entity.CreditApplication;
import com.bna.digitalbanking.credit.scoring.entity.CreditScoringEvaluation;
import org.springframework.stereotype.Service;

@Service
public class EvaluationComparisonService {
    public EvaluationComparison compare(
            CreditApplication application,
            CreditScoringEvaluation scoring,
            AiEvaluation evaluation,
            EvaluationComparison previous) {
        String javaEligibility = application.getSimulationSnapshot() == null
                || application.getSimulationSnapshot().getStatus() == null
            ? "INCONCLUSIVE"
            : application.getSimulationSnapshot().getStatus();
        String javaRisk = scoring.getRiskLevel().name();
        Boolean eligibilityAgreement = null;
        Boolean riskAgreement = null;
        List<String> explanations = new ArrayList<>();

        if (evaluation.getStatus() != AiEvaluationStatus.SUCCESS) {
            explanations.add(evaluation.getMessage());
        } else {
            if (evaluation.getEligibilityStatus() == AiEligibilityStatus.INCONCLUSIVE
                    || "INCONCLUSIVE".equals(javaEligibility)) {
                explanations.add("Éligibilité : comparaison impossible faute de conclusion sur l’un des résultats.");
            } else {
                eligibilityAgreement = javaEligibility.equals(evaluation.getEligibilityStatus().name());
                explanations.add(eligibilityAgreement
                    ? "Éligibilité : les résultats Java et IA sont concordants."
                    : "Éligibilité : divergence entre le résultat Java et la proposition IA.");
            }
            if (evaluation.getRiskLevel() == AiRiskLevel.UNKNOWN) {
                explanations.add("Risque : comparaison impossible, le niveau IA est indéterminé.");
            } else {
                riskAgreement = javaRisk.equals(evaluation.getRiskLevel().name());
                explanations.add(riskAgreement
                    ? "Risque : les niveaux Java et IA sont concordants."
                    : "Risque : divergence entre les niveaux Java et IA.");
            }
        }

        EvaluationComparison comparison = previous == null
            ? new EvaluationComparison(application)
            : previous;
        comparison.update(
            javaEligibility,
            evaluation.getEligibilityStatus(),
            eligibilityAgreement,
            javaRisk,
            evaluation.getRiskLevel(),
            riskAgreement,
            scoring.isProvisional(),
            explanations);
        return comparison;
    }
}
