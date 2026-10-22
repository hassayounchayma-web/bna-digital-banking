package com.bna.digitalbanking.simulation;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SimulationParameterInitializer implements CommandLineRunner {
    private final TmmRepository tmmRepository;
    private final CreditMarginRepository marginRepository;

    public SimulationParameterInitializer(
            TmmRepository tmmRepository,
            CreditMarginRepository marginRepository) {
        this.tmmRepository = tmmRepository;
        this.marginRepository = marginRepository;
    }

    @Override
    public void run(String... args) {
        if (!tmmRepository.existsById(1L)) {
            tmmRepository.save(new TmmEntity(1L, new BigDecimal("7.49")));
        }
        defaultMargins().forEach(this::addIfMissing);
    }

    private void addIfMissing(CreditMarginRequest parameter) {
        if (marginRepository.findByScenarioAndCreditType(parameter.scenario(), parameter.creditType()).isEmpty()) {
            marginRepository.save(new CreditMarginEntity(
                parameter.scenario(),
                parameter.creditType(),
                parameter.marginPercent(),
                parameter.maxMonths()));
        }
    }

    private static List<CreditMarginRequest> defaultMargins() {
        return List.of(
            margin(CreditScenario.SANS_RACHAT, "Crédit Direct Dépenses Courantes", 36, "2.25"),
            margin(CreditScenario.SANS_RACHAT, "Crédit Direct Aménagement", 60, "2.50"),
            margin(CreditScenario.SANS_RACHAT, "Crédit Direct Acquisition Véhicule Neuf", 84, "3.00"),
            margin(CreditScenario.SANS_RACHAT, "Crédit Direct Acquisition Véhicule Occasion", 60, "3.00"),
            margin(CreditScenario.SANS_RACHAT, "Crédit Direct Aménagement Hypothécaire (Durée ≤ 7 ans)", 84, "3.00"),
            margin(CreditScenario.SANS_RACHAT, "Crédit Direct Aménagement Hypothécaire (Durée > 7 ans et ≤ 12 ans)", 144, "3.25"),
            margin(CreditScenario.SANS_RACHAT, "Crédit Direct Acquisition Logement (Durée ≤ 7 ans)", 84, "3.00"),
            margin(CreditScenario.SANS_RACHAT, "Crédit Direct Acquisition Logement (Durée > 7 ans et ≤ 15 ans)", 180, "3.25"),
            margin(CreditScenario.SANS_RACHAT, "Crédit Direct Construction (Durée ≤ 7 ans)", 84, "3.00"),
            margin(CreditScenario.SANS_RACHAT, "Crédit Direct Construction (Durée > 7 ans et ≤ 15 ans)", 180, "3.25"),
            margin(CreditScenario.SANS_RACHAT, "Crédit Direct Achat Terrain (Durée ≤ 7 ans)", 84, "3.00"),
            margin(CreditScenario.SANS_RACHAT, "Crédit Direct Achat Terrain (Durée > 7 ans et ≤ 15 ans)", 180, "3.25"),
            margin(CreditScenario.RACHAT_INTERNE, "Crédit Direct Dépenses Courantes", 36, "3.50"),
            margin(CreditScenario.RACHAT_INTERNE, "Crédit Direct Aménagement", 60, "3.75"),
            margin(CreditScenario.RACHAT_INTERNE, "Crédit Direct Acquisition Véhicule Neuf", 84, "3.00"),
            margin(CreditScenario.RACHAT_INTERNE, "Crédit Direct Acquisition Véhicule Occasion", 60, "3.00"),
            margin(CreditScenario.RACHAT_INTERNE, "Crédit Direct Aménagement Hypothécaire (Durée ≤ 7 ans)", 84, "3.00"),
            margin(CreditScenario.RACHAT_INTERNE, "Crédit Direct Aménagement Hypothécaire (Durée > 7 ans et ≤ 12 ans)", 144, "3.25"),
            margin(CreditScenario.RACHAT_INTERNE, "Crédit Direct Acquisition Logement (Durée ≤ 7 ans)", 84, "3.00"),
            margin(CreditScenario.RACHAT_INTERNE, "Crédit Direct Acquisition Logement (Durée > 7 ans et ≤ 15 ans)", 180, "3.25"),
            margin(CreditScenario.RACHAT_INTERNE, "Crédit Direct Construction (Durée ≤ 7 ans)", 84, "3.00"),
            margin(CreditScenario.RACHAT_INTERNE, "Crédit Direct Construction (Durée > 7 ans et ≤ 15 ans)", 180, "3.25"),
            margin(CreditScenario.RACHAT_INTERNE, "Crédit Direct Achat Terrain (Durée ≤ 7 ans)", 84, "3.00"),
            margin(CreditScenario.RACHAT_INTERNE, "Crédit Direct Achat Terrain (Durée > 7 ans et ≤ 15 ans)", 180, "3.25"),
            margin(CreditScenario.RACHAT_EXTERNE, "Crédit Direct Dépenses Courantes", 36, "2.25"),
            margin(CreditScenario.RACHAT_EXTERNE, "Crédit Direct Aménagement", 60, "2.50"));
    }

    private static CreditMarginRequest margin(
            CreditScenario scenario, String creditType, int maxMonths, String marginPercent) {
        return new CreditMarginRequest(
            scenario, creditType, new BigDecimal(marginPercent), maxMonths);
    }
}
