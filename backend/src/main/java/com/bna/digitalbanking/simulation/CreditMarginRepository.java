package com.bna.digitalbanking.simulation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditMarginRepository extends JpaRepository<CreditMarginEntity, Long> {
    List<CreditMarginEntity> findByScenarioOrderByCreditTypeAsc(CreditScenario scenario);

    List<CreditMarginEntity> findAllByOrderByScenarioAscCreditTypeAsc();

    Optional<CreditMarginEntity> findByScenarioAndCreditType(CreditScenario scenario, String creditType);
}
