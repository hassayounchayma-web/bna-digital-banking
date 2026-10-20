package com.bna.digitalbanking.credit.ai.service;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import com.bna.digitalbanking.credit.ai.dto.AiAssessmentPayload;
import com.bna.digitalbanking.credit.ai.entity.AiEligibilityStatus;
import com.bna.digitalbanking.credit.ai.entity.AiRiskLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class AiAssessmentParser {
    private static final int MAX_ITEMS = 20;
    private static final int MAX_TEXT_LENGTH = 500;

    private final ObjectMapper objectMapper;

    public AiAssessmentParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedAiAssessment parse(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("La réponse IA est vide.");
        }
        AiAssessmentPayload payload;
        try {
            payload = objectMapper.readValue(content, AiAssessmentPayload.class);
        } catch (IOException exception) {
            throw new IllegalArgumentException("La réponse IA n’est pas un JSON valide.", exception);
        }
        if (payload.eligibilityStatus() == null || payload.riskLevel() == null) {
            throw new IllegalArgumentException("La réponse IA ne contient pas les statuts requis.");
        }
        AiEligibilityStatus eligibility = parseEnum(
            AiEligibilityStatus.class, payload.eligibilityStatus(), "eligibility_status");
        AiRiskLevel risk = parseEnum(AiRiskLevel.class, payload.riskLevel(), "risk_level");
        return new ParsedAiAssessment(
            eligibility,
            risk,
            validateList(payload.reasons(), "reasons"),
            validateList(payload.missingInformation(), "missing_information"),
            validateList(payload.warnings(), "warnings"));
    }

    private <T extends Enum<T>> T parseEnum(Class<T> enumType, String value, String property) {
        try {
            return Enum.valueOf(enumType, value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Valeur non autorisée pour " + property + ".", exception);
        }
    }

    private List<String> validateList(List<String> values, String property) {
        if (values == null || values.size() > MAX_ITEMS) {
            throw new IllegalArgumentException("La liste " + property + " est absente ou dépasse sa limite.");
        }
        return values.stream()
            .map(value -> {
                if (value == null || value.isBlank() || value.length() > MAX_TEXT_LENGTH) {
                    throw new IllegalArgumentException("Un élément de " + property + " est invalide.");
                }
                return value.strip();
            })
            .toList();
    }

    public record ParsedAiAssessment(
        AiEligibilityStatus eligibilityStatus,
        AiRiskLevel riskLevel,
        List<String> reasons,
        List<String> missingInformation,
        List<String> warnings
    ) {
    }
}
