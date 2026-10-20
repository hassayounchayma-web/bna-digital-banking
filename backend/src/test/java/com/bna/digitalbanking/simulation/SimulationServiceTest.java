package com.bna.digitalbanking.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SimulationServiceTest {
    private static final String CURRENT_EXPENSES = "Crédit Direct Dépenses Courantes";

    private final TestRateProvider parameterService = new TestRateProvider();
    private final SimulationService simulationService = new SimulationService(parameterService);

    @Test
    void calculatesSansRachatCapacityAndRequestedPayment() {
        configure(CreditScenario.SANS_RACHAT, CURRENT_EXPENSES, 36, "2.25");
        parameterService.tmmAnnualRate = new BigDecimal("0.0749");

        SimulationResult result = simulationService.simulateSansRachat(new SansRachatRequest(
            bd("2000"), bd("0"), bd("100"), bd("100"),
            CURRENT_EXPENSES, 12, bd("5000")));

        assertThat(result.statut()).isEqualTo("ELIGIBLE");
        assertThat(result.totalRevenuNetMoyen()).isEqualByComparingTo("2000.00");
        assertThat(result.tauxEndettement()).isEqualByComparingTo("55.00");
        assertThat(result.gf()).isEqualByComparingTo("65.00");
        assertThat(result.plafondEndettementMensuel()).isEqualByComparingTo("1100.00");
        assertThat(result.availableMensualite()).isEqualByComparingTo("900.00");
        assertThat(result.tauxAnnuel()).isEqualByComparingTo("9.74");
        assertThat(result.mensualite()).isEqualByComparingTo("438.98");
        assertThat(result.totalEndettementMensuel()).isEqualByComparingTo("638.98");
        assertThat(result.tauxEndettementCalcule()).isEqualByComparingTo("31.95");
        assertThat(result.montantMaxCredit()).isGreaterThan(bd("5000"));
    }

    @Test
    void rejectsDurationAboveConfiguredMaximum() {
        configure(CreditScenario.SANS_RACHAT, CURRENT_EXPENSES, 36, "2.25");

        SimulationResult result = simulationService.simulateSansRachat(new SansRachatRequest(
            bd("2000"), bd("0"), bd("0"), bd("0"),
            CURRENT_EXPENSES, 37, null));

        assertThat(result.statut()).isEqualTo("HORS_LIMITES");
        assertThat(result.error()).isEqualTo("Durée max autorisée : 36 mois");
    }

    @Test
    void marksRequestedAmountAboveMaximumWithoutChangingAvailableMonthlyCapacity() {
        configure(CreditScenario.SANS_RACHAT, CURRENT_EXPENSES, 36, "2.25");
        parameterService.tmmAnnualRate = new BigDecimal("0.0749");

        SimulationResult result = simulationService.simulateSansRachat(new SansRachatRequest(
            bd("2000"), bd("0"), bd("100"), bd("100"),
            CURRENT_EXPENSES, 12, bd("50000")));

        assertThat(result.statut()).isEqualTo("HORS_LIMITES");
        assertThat(result.mensualite()).isEqualByComparingTo("900.00");
        assertThat(result.error()).contains("Montant demandé dépasse le maximum");
    }

    @Test
    void reportsInsufficientCapacityForInternalBuyout() {
        configure(CreditScenario.RACHAT_INTERNE, CURRENT_EXPENSES, 36, "3.50");
        parameterService.tmmAnnualRate = new BigDecimal("0.0749");

        SimulationResult result = simulationService.simulateRachatInterne(new RachatInterneRequest(
            bd("1000"), bd("0"), bd("300"), bd("100"), bd("2000"),
            CURRENT_EXPENSES, 12, null));

        assertThat(result.statut()).isEqualTo("CAPACITE_INSUFFISANTE");
        assertThat(result.error()).isEqualTo("Capacité Insuffisante");
        assertThat(result.montantMaxCredit()).isEqualByComparingTo("0.00");
        assertThat(result.availableMensualite()).isEqualByComparingTo("100.00");
    }

    @Test
    void calculatesExternalBuyoutUsingExternalDebtBands() {
        configure(CreditScenario.RACHAT_EXTERNE, CURRENT_EXPENSES, 36, "2.25");
        parameterService.tmmAnnualRate = new BigDecimal("0.0749");

        SimulationResult result = simulationService.simulateRachatExterne(new RachatExterneRequest(
            bd("2500"), bd("0"), bd("200"), bd("1000"),
            CURRENT_EXPENSES, 12, bd("3000")));

        assertThat(result.statut()).isEqualTo("ELIGIBLE");
        assertThat(result.tauxEndettement()).isEqualByComparingTo("55.00");
        assertThat(result.gf()).isEqualByComparingTo("65.00");
        assertThat(result.availableMensualite()).isEqualByComparingTo("1175.00");
        assertThat(result.reliquatCreditApresRachat()).isEqualByComparingTo("2000.00");
        assertThat(result.totalEndettementMensuel()).isEqualByComparingTo("463.39");
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
