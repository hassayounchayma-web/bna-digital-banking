package com.bna.digitalbanking.simulation;

import java.math.BigDecimal;

public record CreditRateConfig(int maxMonths, BigDecimal annualMarginPercent) {
}
