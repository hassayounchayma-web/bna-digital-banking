package com.bna.digitalbanking.simulation;

import java.math.BigDecimal;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record SansRachatRequest(
    @NotNull @PositiveOrZero BigDecimal salaireMensuelMoyen,
    @NotNull @PositiveOrZero BigDecimal loyerMensuel,
    @NotNull @PositiveOrZero BigDecimal retenuesSed,
    @NotNull @PositiveOrZero BigDecimal retenuesCreditsBna,
    @NotBlank @Size(max = 255) String typeCredit,
    @Min(1) int dureeMois,
    @Positive BigDecimal montantCreditDemande
) {
}
