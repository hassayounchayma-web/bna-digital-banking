package com.bna.digitalbanking.credit.scoring.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import com.bna.digitalbanking.credit.application.entity.CreditApplication;
import com.bna.digitalbanking.credit.application.entity.CreditSimulationSnapshot;
import com.bna.digitalbanking.credit.scoring.entity.CreditRiskLevel;
import com.bna.digitalbanking.credit.scoring.entity.CreditScoringEvaluation;
import com.bna.digitalbanking.credit.scoring.entity.ScoringFactor;
import org.springframework.stereotype.Service;

@Service
public class CreditScoringService {
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal DEBT_WEIGHT = new BigDecimal("25");
    private static final BigDecimal AVAILABLE_INCOME_WEIGHT = new BigDecimal("20");
    private static final BigDecimal INCOME_STABILITY_WEIGHT = new BigDecimal("34");
    private static final BigDecimal SAVINGS_REGULARITY_WEIGHT = new BigDecimal("21");

    private final CreditScoringProperties properties;

    public CreditScoringService(CreditScoringProperties properties) {
        this.properties = properties;
    }

    public CreditScoringEvaluation evaluate(
            CreditApplication application,
            CreditSimulationSnapshot simulation) {
        List<ScoringFactor> factors = new ArrayList<>();
        factors.add(debtFactor(simulation));
        factors.add(availableIncomeFactor(simulation));
        factors.add(incomeStabilityFactor(application.getIncomeStabilityMonths()));
        factors.add(savingsFactor(application.getSavingsRegularityMonthsLast12Months()));

        BigDecimal availableWeight = factors.stream()
            .filter(ScoringFactor::isAvailable)
            .map(ScoringFactor::getMaximumPoints)
            .reduce(ZERO, BigDecimal::add);
        BigDecimal awardedPoints = factors.stream()
            .filter(ScoringFactor::isAvailable)
            .map(ScoringFactor::getPoints)
            .reduce(ZERO, BigDecimal::add);
        BigDecimal score = availableWeight.signum() == 0
            ? ZERO
            : awardedPoints.multiply(HUNDRED).divide(availableWeight, 2, RoundingMode.HALF_UP);
        boolean provisional = factors.stream().anyMatch(factor -> !factor.isAvailable());

        return new CreditScoringEvaluation(
            application,
            score,
            riskLevel(score),
            provisional,
            factors);
    }

    private ScoringFactor debtFactor(CreditSimulationSnapshot simulation) {
        BigDecimal debtRate = simulation.getCalculatedDebtRatePercent();
        if (debtRate == null) {
            return missing("DEBT", "Endettement", DEBT_WEIGHT,
                "Le taux d’endettement après simulation n’est pas disponible.");
        }
        BigDecimal normalizedDebt = clamp(debtRate, ZERO, HUNDRED);
        BigDecimal points = DEBT_WEIGHT.multiply(HUNDRED.subtract(normalizedDebt))
            .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        return assessed("DEBT", "Endettement", DEBT_WEIGHT, points,
            "Taux après simulation : " + debtRate.stripTrailingZeros().toPlainString() + " %",
            "Le barème fictif diminue linéairement les points lorsque le taux d’endettement augmente.");
    }

    private ScoringFactor availableIncomeFactor(CreditSimulationSnapshot simulation) {
        BigDecimal income = simulation.getNetIncome();
        BigDecimal capacity = simulation.getMonthlyCapacity();
        BigDecimal payment = simulation.getMonthlyPayment();
        BigDecimal fullScoreRatio = properties.getAvailableIncomeFullScoreRatio();
        if (income == null || income.signum() <= 0 || capacity == null || payment == null
                || fullScoreRatio == null || fullScoreRatio.signum() <= 0) {
            return missing("AVAILABLE_INCOME", "Revenu disponible", AVAILABLE_INCOME_WEIGHT,
                "Le revenu disponible après mensualité n’est pas calculable.");
        }
        BigDecimal remainder = capacity.subtract(payment).max(ZERO);
        BigDecimal ratio = remainder.divide(income, 8, RoundingMode.HALF_UP);
        BigDecimal points = AVAILABLE_INCOME_WEIGHT.multiply(ratio)
            .divide(fullScoreRatio, 2, RoundingMode.HALF_UP)
            .min(AVAILABLE_INCOME_WEIGHT);
        return assessed(
            "AVAILABLE_INCOME",
            "Revenu disponible",
            AVAILABLE_INCOME_WEIGHT,
            points,
            remainder.setScale(2, RoundingMode.HALF_UP).toPlainString() + " DT par mois",
            "Points proportionnels à la part du revenu retenu restant après la mensualité demandée.");
    }

    private ScoringFactor incomeStabilityFactor(Integer months) {
        if (months == null) {
            return missing("INCOME_STABILITY", "Stabilité des revenus", INCOME_STABILITY_WEIGHT,
                "Aucun historique mensuel de revenus SALARY n’est disponible pour établir une stabilité.");
        }
        int fullScoreMonths = Math.max(1, properties.getIncomeStabilityFullScoreMonths());
        BigDecimal points = INCOME_STABILITY_WEIGHT
            .multiply(BigDecimal.valueOf(months))
            .divide(BigDecimal.valueOf(fullScoreMonths), 2, RoundingMode.HALF_UP)
            .min(INCOME_STABILITY_WEIGHT);
        return assessed(
            "INCOME_STABILITY",
            "Stabilité des revenus",
            INCOME_STABILITY_WEIGHT,
            points,
            months + " mois consécutifs observés",
            "Calculé à partir des opérations de revenus SALARY enregistrées, plafonné à "
                + fullScoreMonths + " mois selon le barème de démonstration.");
    }

    private ScoringFactor savingsFactor(Integer regularMonths) {
        if (regularMonths == null) {
            return missing("SAVINGS_REGULARITY", "Régularité de l’épargne", SAVINGS_REGULARITY_WEIGHT,
                "Aucun virement observé vers le compte épargne sur la période disponible.");
        }
        int fullScoreMonths = Math.max(1, properties.getSavingsFullScoreMonths());
        BigDecimal points = SAVINGS_REGULARITY_WEIGHT
            .multiply(BigDecimal.valueOf(regularMonths))
            .divide(BigDecimal.valueOf(fullScoreMonths), 2, RoundingMode.HALF_UP)
            .min(SAVINGS_REGULARITY_WEIGHT);
        return assessed(
            "SAVINGS_REGULARITY",
            "Régularité de l’épargne",
            SAVINGS_REGULARITY_WEIGHT,
            points,
            regularMonths + " mois avec virement observé sur 12",
            "Calculé à partir des virements entrants enregistrés sur le compte épargne pendant les 12 derniers mois.");
    }

    private ScoringFactor missing(String code, String label, BigDecimal weight, String explanation) {
        return new ScoringFactor(code, label, null, weight, null, explanation);
    }

    private ScoringFactor assessed(
            String code,
            String label,
            BigDecimal weight,
            BigDecimal points,
            String inputValue,
            String explanation) {
        return new ScoringFactor(code, label, points, weight, inputValue, explanation);
    }

    private CreditRiskLevel riskLevel(BigDecimal score) {
        if (score.compareTo(new BigDecimal("75")) >= 0) {
            return CreditRiskLevel.LOW;
        }
        if (score.compareTo(new BigDecimal("50")) >= 0) {
            return CreditRiskLevel.MODERATE;
        }
        return CreditRiskLevel.HIGH;
    }

    private BigDecimal clamp(BigDecimal value, BigDecimal minimum, BigDecimal maximum) {
        return value.max(minimum).min(maximum);
    }
}
