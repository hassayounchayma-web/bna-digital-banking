package com.bna.digitalbanking.credit.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApplicationDecisionRequest(
    @NotNull ApplicationDecision decision,
    @jakarta.validation.constraints.NotBlank @Size(max = 1000) String reason
) {
}
