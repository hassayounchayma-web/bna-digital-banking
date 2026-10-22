package com.bna.digitalbanking.simulation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditParameterService implements CreditRateProvider {
    private static final BigDecimal MAX_PERCENT = new BigDecimal("100");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final Set<String> EXTERNAL_BUYOUT_PRODUCTS = Set.of(
        "Crédit Direct Dépenses Courantes",
        "Crédit Direct Aménagement");

    private final TmmRepository tmmRepository;
    private final CreditMarginRepository marginRepository;

    public CreditParameterService(TmmRepository tmmRepository, CreditMarginRepository marginRepository) {
        this.tmmRepository = tmmRepository;
        this.marginRepository = marginRepository;
    }

    @Override
    public BigDecimal getTmmAnnualRate() {
        return tmmRepository.findById(1L)
            .map(TmmEntity::getValuePercent)
            .map(value -> value.divide(HUNDRED, 12, RoundingMode.HALF_UP))
            .orElseThrow(() -> new SimulationConfigException(
                HttpStatus.SERVICE_UNAVAILABLE, "Le TMM n'est pas configuré"));
    }

    @Override
    public CreditRateConfig getRateConfig(CreditScenario scenario, String creditType) {
        if (!isAllowedProduct(scenario, creditType)) {
            throw new SimulationConfigException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Produit non autorisé pour ce scénario");
        }
        CreditMarginEntity margin = marginRepository.findByScenarioAndCreditType(scenario, creditType)
            .orElseThrow(() -> new SimulationConfigException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Aucune marge configurée pour ce produit et ce scénario"));
        return new CreditRateConfig(
            margin.getMaxMonths(),
            margin.getMarginPercent().divide(HUNDRED, 12, RoundingMode.HALF_UP));
    }

    @Transactional(readOnly = true)
    public Map<String, CreditRateConfig> getCatalogue(CreditScenario scenario) {
        return marginRepository.findByScenarioOrderByCreditTypeAsc(scenario).stream()
            .filter(margin -> isAllowedProduct(scenario, margin.getCreditType()))
            .collect(Collectors.toMap(
                CreditMarginEntity::getCreditType,
                margin -> new CreditRateConfig(
                    margin.getMaxMonths(),
                    margin.getMarginPercent().divide(HUNDRED, 12, RoundingMode.HALF_UP)),
                (first, duplicate) -> first,
                java.util.LinkedHashMap::new));
    }

    private static boolean isAllowedProduct(CreditScenario scenario, String creditType) {
        return scenario != CreditScenario.RACHAT_EXTERNE
            || EXTERNAL_BUYOUT_PRODUCTS.contains(creditType);
    }

    @Transactional(readOnly = true)
    public SimulationParametersResponse getParameters() {
        BigDecimal tmm = tmmRepository.findById(1L).map(TmmEntity::getValuePercent).orElse(null);
        List<CreditMarginResponse> margins = marginRepository.findAllByOrderByScenarioAscCreditTypeAsc()
            .stream()
            .map(CreditMarginResponse::from)
            .toList();
        return new SimulationParametersResponse(tmm, margins);
    }

    @Transactional
    public BigDecimal updateTmm(BigDecimal valuePercent) {
        validatePercent(valuePercent);
        TmmEntity entity = tmmRepository.findById(1L)
            .orElseGet(() -> new TmmEntity(1L, valuePercent));
        entity.updateValue(valuePercent);
        return tmmRepository.save(entity).getValuePercent();
    }

    @Transactional
    public CreditMarginResponse saveMargin(CreditMarginRequest request) {
        String creditType = request.creditType().strip();
        validatePercent(request.marginPercent());
        CreditMarginEntity entity = marginRepository
            .findByScenarioAndCreditType(request.scenario(), creditType)
            .orElseGet(() -> new CreditMarginEntity(
                request.scenario(), creditType, request.marginPercent(), request.maxMonths()));
        entity.update(request.marginPercent(), request.maxMonths());
        return CreditMarginResponse.from(marginRepository.save(entity));
    }

    private static void validatePercent(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.compareTo(MAX_PERCENT) > 0
                || value.scale() > 4) {
            throw new SimulationConfigException(
                HttpStatus.BAD_REQUEST,
                "Le taux doit être compris entre 0 et 100 avec au maximum 4 décimales");
        }
    }
}
