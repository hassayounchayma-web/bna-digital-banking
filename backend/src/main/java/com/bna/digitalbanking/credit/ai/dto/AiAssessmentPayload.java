package com.bna.digitalbanking.credit.ai.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AiAssessmentPayload(
    @JsonProperty("eligibility_status") String eligibilityStatus,
    @JsonProperty("risk_level") String riskLevel,
    List<String> reasons,
    @JsonProperty("missing_information") List<String> missingInformation,
    List<String> warnings
) {
}
