package com.bna.digitalbanking.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SimulationServiceTest {
    private static final String CURRENT_EXPENSES = "Crédit Direct Dépenses Courantes";
    private static final String HOME_IMPROVEMENT = "Crédit Direct Aménagement";

    private final TestRateProvider parameterService = new TestRateProvider();
    private final SimulationService simulationService = new SimulationService(parameterService);

    @Test
    void caseACalculatesSansRachatReferenceValues() {
        configure(CreditScenario.SANS_RACHAT, CURRENT_EXPENSES, 36, "2.25");

        SimulationResult result = simulationService.simulateSansRachat(new SansRachatRequest(
            bd("2500"), bd("400"), bd("200"), bd("250"),
            CURRENT_EXPENSES, 36, bd("10000")));

        assertThat(result.statut()).isEqualTo("ELIGIBLE");
        assertThat(result.totalRevenuNetMoyen()).isEqualByComparingTo("2700.00");
        assertThat(result.tauxEndettement()).isEqualByComparingTo("55.00");
        assertThat(result.gf()).isEqualByComparingTo("65.00");
        assertThat(result.plafondEndettementMensuel()).isEqualByComparingTo("1485.00");
        assertThat(result.plafondEndettementMensuel().subtract(result.availableMensualite()))
            .isEqualByComparingTo("450.00");
        assertThat(result.totalRetenues()).isEqualByComparingTo("771.45");
        assertThat(result.availableMensualite()).isEqualByComparingTo("1035.00");
        assertThat(result.tauxAnnuel()).isEqualByComparingTo("9.74");
        assertThat(result.montantMaxCredit()).isEqualByComparingTo("32197.60");
        assertThat(result.mensualite()).isEqualByComparingTo("321.45");
    }

    @Test
    void caseBCalculatesRachatInterneReferenceValuesAndCoversCapital() {
        configure(CreditScenario.RACHAT_INTERNE, CURRENT_EXPENSES, 36, "3.50");

        SimulationResult result = simulationService.simulateRachatInterne(new RachatInterneRequest(
            bd("2500"), bd("400"), bd("200"), bd("150"), bd("12000"),
            CURRENT_EXPENSES, 36, bd("12000")));

        assertThat(result.statut()).isEqualTo("ELIGIBLE");
        assertThat(result.totalRevenuNetMoyen()).isEqualByComparingTo("2700.00");
        assertThat(result.tauxEndettement()).isEqualByComparingTo("55.00");
        assertThat(result.gf()).isEqualByComparingTo("65.00");
        assertThat(result.plafondEndettementMensuel()).isEqualByComparingTo("1485.00");
        assertThat(result.plafondEndettementMensuel().subtract(result.availableMensualite()))
            .isEqualByComparingTo("350.00");
        assertThat(result.totalRetenues()).isEqualByComparingTo("742.81");
        assertThat(result.availableMensualite()).isEqualByComparingTo("1135.00");
        assertThat(result.tauxAnnuel()).isEqualByComparingTo("10.99");
        assertThat(result.montantMaxCredit()).isEqualByComparingTo("34673.45");
        assertThat(result.mensualite()).isEqualByComparingTo("392.81");
        assertThat(result.reliquatCreditApresRachat()).isEqualByComparingTo("0.00");
    }

    @Test
    void caseCCalculatesRachatExterneReferenceValuesAndCoversCapital() {
        configure(CreditScenario.RACHAT_EXTERNE, HOME_IMPROVEMENT, 60, "2.50");

        SimulationResult result = simulationService.simulateRachatExterne(new RachatExterneRequest(
            bd("2200"), bd("0"), bd("250"), bd("15000"),
            HOME_IMPROVEMENT, 60, bd("15000")));

        assertThat(result.statut()).isEqualTo("ELIGIBLE");
        assertThat(result.totalRevenuNetMoyen()).isEqualByComparingTo("2200.00");
        assertThat(result.tauxEndettement()).isEqualByComparingTo("55.00");
        assertThat(result.gf()).isEqualByComparingTo("65.00");
        assertThat(result.plafondEndettementMensuel()).isEqualByComparingTo("1210.00");
        assertThat(result.plafondEndettementMensuel().subtract(result.availableMensualite()))
            .isEqualByComparingTo("250.00");
        assertThat(result.totalRetenues()).isEqualByComparingTo("568.63");
        assertThat(result.availableMensualite()).isEqualByComparingTo("960.00");
        assertThat(result.tauxAnnuel()).isEqualByComparingTo("9.99");
        assertThat(result.montantMaxCredit()).isEqualByComparingTo("45193.22");
        assertThat(result.mensualite()).isEqualByComparingTo("318.63");
        assertThat(result.reliquatCreditApresRachat()).isEqualByComparingTo("0.00");
    }

    @ParameterizedTest
    @CsvSource({
        "999.99,40", "1000,50", "1499.99,50", "1500,50", "1999.99,50",
        "2000,55", "2999.99,55", "3000,60", "3999.99,60", "4000,65"
    })
    void appliesStandardDebtRateBoundaries(String income, String expectedRate) {
        configure(CreditScenario.SANS_RACHAT, CURRENT_EXPENSES, 36, "2.25");

        SimulationResult result = simulationService.simulateSansRachat(new SansRachatRequest(
            bd(income), bd("0"), bd("0"), bd("0"), CURRENT_EXPENSES, 12, null));

        assertThat(result.tauxEndettement()).isEqualByComparingTo(expectedRate);
    }

    @ParameterizedTest
    @CsvSource({
        "999.99,50", "1000,60", "1999.99,60", "2000,65",
        "2999.99,65", "3000,70", "3999.99,70", "4000,75"
    })
    void appliesStandardSafeguardBoundaries(String salary, String expectedSafeguard) {
        configure(CreditScenario.SANS_RACHAT, CURRENT_EXPENSES, 36, "2.25");

        SimulationResult result = simulationService.simulateSansRachat(new SansRachatRequest(
            bd(salary), bd("0"), bd("0"), bd("0"), CURRENT_EXPENSES, 12, null));

        assertThat(result.gf()).isEqualByComparingTo(expectedSafeguard);
    }

    @ParameterizedTest
    @CsvSource({
        "1500,50", "1999.99,50", "2000,55", "2999.99,55",
        "3000,60", "3999.99,60", "4000,65"
    })
    void appliesExternalDebtRateBoundaries(String income, String expectedRate) {
        configure(CreditScenario.RACHAT_EXTERNE, CURRENT_EXPENSES, 36, "2.25");

        SimulationResult result = simulationService.simulateRachatExterne(new RachatExterneRequest(
            bd(income), bd("0"), bd("0"), bd("1"), CURRENT_EXPENSES, 12, null));

        assertThat(result.tauxEndettement()).isEqualByComparingTo(expectedRate);
    }

    @ParameterizedTest
    @CsvSource({
        "1500,60", "1999.99,60", "2000,65", "2999.99,65",
        "3000,70", "3999.99,70", "4000,75"
    })
    void appliesExternalSafeguardBoundaries(String salary, String expectedSafeguard) {
        configure(CreditScenario.RACHAT_EXTERNE, CURRENT_EXPENSES, 36, "2.25");

        SimulationResult result = simulationService.simulateRachatExterne(new RachatExterneRequest(
            bd(salary), bd("0"), bd("0"), bd("1"), CURRENT_EXPENSES, 12, null));

        assertThat(result.gf()).isEqualByComparingTo(expectedSafeguard);
    }

    @Test
    void rejectsExternalBuyoutBelowIncomeThreshold() {
        SimulationResult result = simulationService.simulateRachatExterne(new RachatExterneRequest(
            bd("1200"), bd("0"), bd("0"), bd("1"), CURRENT_EXPENSES, 12, null));

        assertThat(result.statut()).isEqualTo("NON_ELIGIBLE");
        assertThat(result.error()).contains("1 500 DT");
        assertThat(result.totalRevenuNetMoyen()).isEqualByComparingTo("1200.00");
        assertThat(result.montantMaxCredit()).isNull();
    }

    @Test
    void flagsUnspecifiedExternalSafeguardWhenRentMakesIncomeEligible() {
        SimulationResult result = simulationService.simulateRachatExterne(new RachatExterneRequest(
            bd("1200"), bd("600"), bd("0"), bd("1"), CURRENT_EXPENSES, 12, null));

        assertThat(result.statut()).isEqualTo("REGLE_A_CLARIFIER");
        assertThat(result.totalRevenuNetMoyen()).isEqualByComparingTo("1500.00");
        assertThat(result.montantMaxCredit()).isNull();
    }

    @Test
    void reportsInsufficientCapacityForZeroOrNegativeAvailableMonthlyCapacity() {
        configure(CreditScenario.SANS_RACHAT, CURRENT_EXPENSES, 36, "2.25");

        SimulationResult result = simulationService.simulateSansRachat(new SansRachatRequest(
            bd("1000"), bd("0"), bd("600"), bd("0"), CURRENT_EXPENSES, 12, null));

        assertThat(result.statut()).isEqualTo("CAPACITE_INSUFFISANTE");
        assertThat(result.availableMensualite()).isEqualByComparingTo("-100.00");
        assertThat(result.montantMaxCredit()).isEqualByComparingTo("0.00");
        assertThat(result.mensualite()).isNull();
    }

    @Test
    void acceptsZeroSalaryWithoutDivisionByZeroOrPositiveMaximum() {
        configure(CreditScenario.SANS_RACHAT, CURRENT_EXPENSES, 36, "2.25");

        SimulationResult result = simulationService.simulateSansRachat(new SansRachatRequest(
            bd("0"), bd("0"), bd("0"), bd("0"), CURRENT_EXPENSES, 12, null));

        assertThat(result.statut()).isEqualTo("CAPACITE_INSUFFISANTE");
        assertThat(result.totalRevenuNetMoyen()).isEqualByComparingTo("0.00");
        assertThat(result.montantMaxCredit()).isEqualByComparingTo("0.00");
        assertThat(result.mensualite()).isNull();
    }

    @Test
    void reportsInsufficientCapacityWhenBuyoutCapitalExceedsMaximumFinancing() {
        configure(CreditScenario.RACHAT_INTERNE, CURRENT_EXPENSES, 36, "3.50");

        SimulationResult result = simulationService.simulateRachatInterne(new RachatInterneRequest(
            bd("1000"), bd("0"), bd("300"), bd("100"), bd("2000"),
            CURRENT_EXPENSES, 12, null));

        assertThat(result.statut()).isEqualTo("CAPACITE_INSUFFISANTE");
        assertThat(result.montantMaxCredit()).isEqualByComparingTo("0.00");
        assertThat(result.mensualite()).isNull();
    }

    @Test
    void rejectsRequestedBuyoutBelowRemainingCapitalForBothScenarios() {
        configure(CreditScenario.RACHAT_INTERNE, CURRENT_EXPENSES, 36, "3.50");
        configure(CreditScenario.RACHAT_EXTERNE, CURRENT_EXPENSES, 36, "2.25");

        SimulationResult internal = simulationService.simulateRachatInterne(new RachatInterneRequest(
            bd("2500"), bd("0"), bd("0"), bd("0"), bd("12000"),
            CURRENT_EXPENSES, 36, bd("11999.99")));
        SimulationResult external = simulationService.simulateRachatExterne(new RachatExterneRequest(
            bd("2500"), bd("0"), bd("0"), bd("12000"),
            CURRENT_EXPENSES, 36, bd("11999.99")));

        assertThat(internal.statut()).isEqualTo("HORS_LIMITES");
        assertThat(internal.error()).contains("supérieur ou égal au capital restant dû");
        assertThat(external.statut()).isEqualTo("HORS_LIMITES");
        assertThat(external.error()).contains("supérieur ou égal au capital restant dû");
    }

    @Test
    void rejectsRequestedBuyoutAboveMaximumForBothScenarios() {
        configure(CreditScenario.RACHAT_INTERNE, CURRENT_EXPENSES, 36, "3.50");
        configure(CreditScenario.RACHAT_EXTERNE, CURRENT_EXPENSES, 36, "2.25");

        SimulationResult internal = simulationService.simulateRachatInterne(new RachatInterneRequest(
            bd("2500"), bd("0"), bd("0"), bd("0"), bd("1000"),
            CURRENT_EXPENSES, 36, bd("1000000")));
        SimulationResult external = simulationService.simulateRachatExterne(new RachatExterneRequest(
            bd("2500"), bd("0"), bd("0"), bd("1000"),
            CURRENT_EXPENSES, 36, bd("1000000")));

        assertThat(internal.statut()).isEqualTo("HORS_LIMITES");
        assertThat(internal.error()).contains("dépasse le maximum");
        assertThat(external.statut()).isEqualTo("HORS_LIMITES");
        assertThat(external.error()).contains("dépasse le maximum");
    }

    @Test
    void rejectsNonPositiveDuration() {
        configure(CreditScenario.SANS_RACHAT, CURRENT_EXPENSES, 36, "2.25");

        SimulationResult result = simulationService.simulateSansRachat(new SansRachatRequest(
            bd("2000"), bd("0"), bd("0"), bd("0"), CURRENT_EXPENSES, 0, null));

        assertThat(result.statut()).isEqualTo("PARAMETRES_INVALIDES");
        assertThat(result.error()).contains("durée doit être strictement positive");
        assertThat(result.montantMaxCredit()).isNull();
    }

    @Test
    void rejectsDurationAboveConfiguredMaximum() {
        configure(CreditScenario.SANS_RACHAT, CURRENT_EXPENSES, 36, "2.25");

        SimulationResult result = simulationService.simulateSansRachat(new SansRachatRequest(
            bd("2000"), bd("0"), bd("0"), bd("0"), CURRENT_EXPENSES, 37, null));

        assertThat(result.statut()).isEqualTo("HORS_LIMITES");
        assertThat(result.error()).isEqualTo("Durée max autorisée : 36 mois");
    }

    @Test
    void allowsMaximumCalculationWithoutRequestedAmount() {
        configure(CreditScenario.SANS_RACHAT, CURRENT_EXPENSES, 36, "2.25");

        SimulationResult result = simulationService.simulateSansRachat(new SansRachatRequest(
            bd("2500"), bd("400"), bd("200"), bd("250"), CURRENT_EXPENSES, 36, null));

        assertThat(result.statut()).isEqualTo("ELIGIBLE");
        assertThat(result.montantMaxCredit()).isEqualByComparingTo("32197.60");
        assertThat(result.mensualite()).isNull();
    }

    @Test
    void usesZeroInterestFormula() {
        assertThat(SimulationService.presentValue(BigDecimal.ZERO, 12, bd("100")))
            .isEqualByComparingTo("1200");
        assertThat(SimulationService.payment(BigDecimal.ZERO, 12, bd("1200")))
            .isEqualByComparingTo("100");
    }

    private void configure(CreditScenario scenario, String creditType, int maxMonths, String marginPercent) {
        parameterService.configs.put(
            scenario + "|" + creditType,
            new CreditRateConfig(maxMonths, new BigDecimal(marginPercent)
                .divide(new BigDecimal("100"))));
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static final class TestRateProvider implements CreditRateProvider {
        private BigDecimal tmmAnnualRate = new BigDecimal("0.0749");
        private final Map<String, CreditRateConfig> configs = new HashMap<>();

        @Override
        public BigDecimal getTmmAnnualRate() {
            return tmmAnnualRate;
        }

        @Override
        public CreditRateConfig getRateConfig(CreditScenario scenario, String creditType) {
            return configs.get(scenario + "|" + creditType);
        }
    }
}
