package com.bna.digitalbanking.simulation;

import java.math.BigDecimal;
import java.util.List;

public record SimulationParametersResponse(BigDecimal tmmPercent, List<CreditMarginResponse> margins) {
}
