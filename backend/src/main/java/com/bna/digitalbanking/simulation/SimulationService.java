package com.bna.digitalbanking.simulation;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class SimulationService {
    private static final MathContext CALCULATION_CONTEXT = MathContext.DECIMAL128;
    private static final BigDecimal ZERO_RATE_THRESHOLD = new BigDecimal("0.000000000001");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWELVE = new BigDecimal("12");
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;

    private final CreditRateProvider parameterService;

    public SimulationService(CreditRateProvider parameterService) {
        this.parameterService = parameterService;
    }

    public SimulationResult simulateSansRachat(SansRachatRequest request) {
        CreditRateConfig config = parameterService.getRateConfig(
            CreditScenario.SANS_RACHAT, request.typeCredit());
        if (request.dureeMois() > config.maxMonths()) {
            return SimulationResult.error(
                "Durée max autorisée : " + config.maxMonths() + " mois");
        }

        BigDecimal income = request.salaireMensuelMoyen().add(request.cinquantePourcentLoyers());
        BigDecimal debtRate = standardDebtRate(income);
        BigDecimal safeguard = standardSafeguard(request.salaireMensuelMoyen());
        BigDecimal ceiling = ceiling(request.salaireMensuelMoyen(), income, debtRate, safeguard);
        BigDecimal available = nonNegative(ceiling.subtract(request.retenuesSed())
            .subtract(request.retenuesCreditsBna()));
        BigDecimal annualRate = parameterService.getTmmAnnualRate().add(config.annualMarginPercent());
        BigDecimal maxCredit = presentValue(annualRate, request.dureeMois(), available);

        SimulationResult result = baseResult(
            income, debtRate, safeguard, ceiling, available, maxCredit, request.dureeMois(), annualRate);
        BigDecimal requested = request.montantCreditDemande();
        if (requested == null || requested.signum() == 0) {
            return withStatusAndMonthly(result, "ELIGIBLE", available);
        }
        if (requested.compareTo(maxCredit.add(new BigDecimal("0.01"))) > 0) {
            return withError(result, available, "HORS_LIMITES",
                "Montant demandé dépasse le maximum (" + money(maxCredit) + " DT)");
        }

        BigDecimal monthly = payment(annualRate, request.dureeMois(), requested);
        BigDecimal total = request.retenuesSed().add(request.retenuesCreditsBna()).add(monthly);
        return withRequestedCredit(result, monthly, total, income, null);
    }

    public SimulationResult simulateRachatInterne(RachatInterneRequest request) {
        CreditRateConfig config = parameterService.getRateConfig(
            CreditScenario.RACHAT_INTERNE, request.typeCredit());
        if (request.dureeMois() > config.maxMonths()) {
            return SimulationResult.error(
                "Durée max autorisée : " + config.maxMonths() + " mois");
        }

        BigDecimal income = request.salaireMensuelMoyen().add(request.cinquantePourcentLoyers());
        BigDecimal debtRate = standardDebtRate(income);
        BigDecimal safeguard = standardSafeguard(request.salaireMensuelMoyen());
        BigDecimal ceiling = ceiling(request.salaireMensuelMoyen(), income, debtRate, safeguard);
        BigDecimal available = nonNegative(ceiling.subtract(request.retenuesSed())
            .subtract(request.retenuesCreditsBnaNonRachete()));
        BigDecimal annualRate = parameterService.getTmmAnnualRate().add(config.annualMarginPercent());
        BigDecimal maxCredit = presentValue(annualRate, request.dureeMois(), available);
        SimulationResult result = baseResult(
            income, debtRate, safeguard, ceiling, available, maxCredit, request.dureeMois(), annualRate);

        if (maxCredit.compareTo(request.capitalRestantARacheter()) < 0) {
            return SimulationResult.capacityInsufficient(result);
        }
        BigDecimal requested = request.montantCreditDemande();
        if (requested == null || requested.signum() == 0) {
            return withStatusAndMonthly(result, "ELIGIBLE", available);
        }
        if (requested.compareTo(maxCredit.add(new BigDecimal("0.01"))) > 0) {
            return withError(result, available, "HORS_LIMITES",
                "Montant demandé dépasse le maximum (" + money(maxCredit) + " DT)");
        }

        BigDecimal monthly = payment(annualRate, request.dureeMois(), requested);
        BigDecimal total = request.retenuesSed()
            .add(request.retenuesCreditsBnaNonRachete()).add(monthly);
        BigDecimal remainingCredit = requested.subtract(request.capitalRestantARacheter());
        return withRequestedCredit(result, monthly, total, income, remainingCredit);
    }

    public SimulationResult simulateRachatExterne(RachatExterneRequest request) {
        CreditRateConfig config = parameterService.getRateConfig(
            CreditScenario.RACHAT_EXTERNE, request.typeCredit());
        if (request.dureeMois() > config.maxMonths()) {
            return SimulationResult.error(
                "Durée max autorisée : " + config.maxMonths() + " mois");
        }

        BigDecimal income = request.salaireMensuelMoyen().add(request.cinquantePourcentLoyers());
        BigDecimal debtRate = externalDebtRate(income);
        BigDecimal safeguard = externalSafeguard(request.salaireMensuelMoyen());
        BigDecimal ceiling = ceiling(request.salaireMensuelMoyen(), income, debtRate, safeguard);
        BigDecimal available = nonNegative(ceiling.subtract(request.retenuesSed()));
        BigDecimal annualRate = parameterService.getTmmAnnualRate().add(config.annualMarginPercent());
        BigDecimal maxCredit = presentValue(annualRate, request.dureeMois(), available);
        SimulationResult result = baseResult(
            income, debtRate, safeguard, ceiling, available, maxCredit, request.dureeMois(), annualRate);

        if (maxCredit.compareTo(request.capitalRestantARacheter()) < 0) {
            return SimulationResult.capacityInsufficient(result);
        }
        BigDecimal requested = request.montantCreditDemande();
        if (requested == null || requested.signum() == 0) {
            return withStatusAndMonthly(result, "ELIGIBLE", available);
        }
        if (requested.compareTo(maxCredit.add(new BigDecimal("0.01"))) > 0) {
            return withError(result, available, "HORS_LIMITES",
                "Montant demandé dépasse le maximum (" + money(maxCredit) + " DT)");
        }

        BigDecimal monthly = payment(annualRate, request.dureeMois(), requested);
        BigDecimal total = request.retenuesSed().add(monthly);
        BigDecimal remainingCredit = requested.subtract(request.capitalRestantARacheter());
        return withRequestedCredit(result, monthly, total, income, remainingCredit);
    }

    static BigDecimal presentValue(BigDecimal annualRate, int months, BigDecimal monthlyPayment) {
        if (months <= 0 || monthlyPayment.signum() <= 0) {
            return ZERO;
        }
        BigDecimal monthlyRate = annualRate.divide(TWELVE, CALCULATION_CONTEXT);
        if (monthlyRate.abs().compareTo(ZERO_RATE_THRESHOLD) < 0) {
            return monthlyPayment.multiply(BigDecimal.valueOf(months), CALCULATION_CONTEXT);
        }
        BigDecimal growth = ONE.add(monthlyRate).pow(months, CALCULATION_CONTEXT);
        BigDecimal discount = ONE.divide(growth, CALCULATION_CONTEXT);
        BigDecimal factor = ONE.subtract(discount).divide(monthlyRate, CALCULATION_CONTEXT);
        return monthlyPayment.multiply(factor, CALCULATION_CONTEXT);
    }

    static BigDecimal payment(BigDecimal annualRate, int months, BigDecimal capital) {
        if (months <= 0 || capital.signum() <= 0) {
            return ZERO;
        }
        BigDecimal monthlyRate = annualRate.divide(TWELVE, CALCULATION_CONTEXT);
        if (monthlyRate.abs().compareTo(ZERO_RATE_THRESHOLD) < 0) {
            return capital.divide(BigDecimal.valueOf(months), CALCULATION_CONTEXT);
        }
        BigDecimal growth = ONE.add(monthlyRate).pow(months, CALCULATION_CONTEXT);
        BigDecimal factor = monthlyRate.multiply(growth, CALCULATION_CONTEXT)
            .divide(growth.subtract(ONE), CALCULATION_CONTEXT);
        return capital.multiply(factor, CALCULATION_CONTEXT);
    }

    private static BigDecimal standardDebtRate(BigDecimal income) {
        if (income.compareTo(new BigDecimal("1000")) < 0) return new BigDecimal("0.40");
        if (income.compareTo(new BigDecimal("2000")) < 0) return new BigDecimal("0.50");
        if (income.compareTo(new BigDecimal("3000")) < 0) return new BigDecimal("0.55");
        if (income.compareTo(new BigDecimal("4000")) < 0) return new BigDecimal("0.60");
        return new BigDecimal("0.65");
    }

    private static BigDecimal standardSafeguard(BigDecimal salary) {
        if (salary.compareTo(new BigDecimal("1000")) < 0) return new BigDecimal("0.50");
        if (salary.compareTo(new BigDecimal("2000")) < 0) return new BigDecimal("0.60");
        if (salary.compareTo(new BigDecimal("3000")) < 0) return new BigDecimal("0.65");
        if (salary.compareTo(new BigDecimal("4000")) < 0) return new BigDecimal("0.70");
        return new BigDecimal("0.75");
    }

    private static BigDecimal externalDebtRate(BigDecimal income) {
        if (income.compareTo(new BigDecimal("2000")) < 0) return new BigDecimal("0.50");
        if (income.compareTo(new BigDecimal("3000")) < 0) return new BigDecimal("0.55");
        if (income.compareTo(new BigDecimal("4000")) < 0) return new BigDecimal("0.60");
        return new BigDecimal("0.65");
    }

    private static BigDecimal externalSafeguard(BigDecimal salary) {
        if (salary.compareTo(new BigDecimal("2000")) < 0) return new BigDecimal("0.60");
        if (salary.compareTo(new BigDecimal("3000")) < 0) return new BigDecimal("0.65");
        if (salary.compareTo(new BigDecimal("4000")) < 0) return new BigDecimal("0.70");
        return new BigDecimal("0.75");
    }

    private static BigDecimal ceiling(
            BigDecimal salary, BigDecimal income, BigDecimal debtRate, BigDecimal safeguard) {
        return income.multiply(debtRate).min(salary.multiply(safeguard));
    }

    private static SimulationResult baseResult(
            BigDecimal income,
            BigDecimal debtRate,
            BigDecimal safeguard,
            BigDecimal ceiling,
            BigDecimal available,
            BigDecimal maxCredit,
            int months,
            BigDecimal annualRate) {
        return new SimulationResult(
            money(income),
            money(debtRate.multiply(HUNDRED)),
            money(safeguard.multiply(HUNDRED)),
            money(ceiling),
            money(available),
            money(available),
            money(maxCredit),
            null,
            months,
            money(annualRate.multiply(HUNDRED)),
            null,
            null,
            null,
            null,
            null,
            null);
    }

    private static SimulationResult withStatusAndMonthly(
            SimulationResult result, String status, BigDecimal monthly) {
        return copy(result, result.montantMaxCredit(), money(monthly), null, null, null, null, status, null);
    }

    private static SimulationResult withError(
            SimulationResult result, BigDecimal monthlyCapacity, String status, String error) {
        return copy(result, result.montantMaxCredit(), money(monthlyCapacity),
            null, null, null, null, status, error);
    }

    private static SimulationResult withRequestedCredit(
            SimulationResult result,
            BigDecimal monthly,
            BigDecimal total,
            BigDecimal income,
            BigDecimal remainingCredit) {
        BigDecimal calculatedRate = income.signum() > 0
            ? money(total.divide(income, CALCULATION_CONTEXT).multiply(HUNDRED))
            : ZERO;
        return copy(result, result.montantMaxCredit(), money(monthly), money(total),
            money(total), calculatedRate, remainingCredit == null ? null : money(remainingCredit),
            "ELIGIBLE", null);
    }

    private static SimulationResult copy(
            SimulationResult result,
            BigDecimal maxCredit,
            BigDecimal monthly,
            BigDecimal totalDebt,
            BigDecimal totalWithholdings,
            BigDecimal calculatedDebtRate,
            BigDecimal remainingCredit,
            String status,
            String error) {
        return new SimulationResult(
            result.totalRevenuNetMoyen(),
            result.tauxEndettement(),
            result.gf(),
            result.plafondEndettementMensuel(),
            result.availableMensualite(),
            result.reliquatCapaciteEffectif(),
            maxCredit,
            monthly,
            result.duree(),
            result.tauxAnnuel(),
            totalDebt,
            totalWithholdings,
            calculatedDebtRate,
            remainingCredit,
            status,
            error);
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        return value.max(ZERO);
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
