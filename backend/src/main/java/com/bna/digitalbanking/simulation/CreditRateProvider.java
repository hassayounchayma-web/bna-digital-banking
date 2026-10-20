package com.bna.digitalbanking.simulation;

import java.math.BigDecimal;

public interface CreditRateProvider {
    BigDecimal getTmmAnnualRate();

    CreditRateConfig getRateConfig(CreditScenario scenario, String creditType);
}
