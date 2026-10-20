package com.bna.digitalbanking.simulation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditParameterService implements CreditRateProvider {
    private static final BigDecimal MAX_PERCENT = new BigDecimal("100");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

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
            .collect(Collectors.toMap(
                CreditMarginEntity::getCreditType,
                margin -> new CreditRateConfig(
                    margin.getMaxMonths(),
                    margin.getMarginPercent().divide(HUNDRED, 12, RoundingMode.HALF_UP)),
                (first, duplicate) -> first,
                java.util.LinkedHashMap::new));
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
