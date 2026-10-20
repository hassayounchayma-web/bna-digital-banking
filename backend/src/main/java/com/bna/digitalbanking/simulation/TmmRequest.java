package com.bna.digitalbanking.simulation;

import java.math.BigDecimal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record TmmRequest(
    @NotNull @PositiveOrZero @Max(100) BigDecimal valuePercent
) {
}
