package com.bna.digitalbanking.credit.ai.service;

import java.net.http.HttpClient;
import java.util.List;
import java.util.stream.Collectors;
import com.bna.digitalbanking.credit.ai.dto.CreditAiFacts;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GeminiProvider implements CreditAiProvider {
    private static final String SYSTEM_PROMPT = """
        Tu aides un évaluateur humain à examiner une demande fictive de crédit.
        Les données reçues sont uniquement des valeurs de démonstration et ne contiennent aucune identité.
        Les champs de stabilité et d’épargne sont dérivés des transactions fictives enregistrées.
        Tu ne dois ni calculer un montant maximal de crédit, ni produire une décision bancaire finale.
        Retourne uniquement un objet JSON avec les clés exactes :
        eligibility_status (ELIGIBLE, INELIGIBLE ou INCONCLUSIVE),
        risk_level (LOW, MODERATE, HIGH ou UNKNOWN),
        reasons (liste de chaînes),
        missing_information (liste de chaînes),
        warnings (liste de chaînes).
        Utilise INCONCLUSIVE et UNKNOWN lorsque les informations ne permettent pas une appréciation.
        """;

    private final RestClient client;
    private final CreditAiProperties properties;
    private final ObjectMapper objectMapper;

    public GeminiProvider(CreditAiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        if (properties.getConnectTimeout().isNegative() || properties.getConnectTimeout().isZero()
                || properties.getReadTimeout().isNegative() || properties.getReadTimeout().isZero()) {
            throw new IllegalStateException("Les délais d’attente IA doivent être strictement positifs.");
        }
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(properties.getConnectTimeout())
            .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());
        this.client = RestClient.builder()
            .baseUrl(properties.getBaseUrl())
            .requestFactory(requestFactory)
            .build();
    }

    @Override
    public boolean isConfigured() {
        return properties.isEnabled()
            && !properties.getApiKey().isBlank()
            && !properties.getModel().isBlank();
    }

    @Override
    public String analyze(CreditAiFacts facts) {
        if (!isConfigured()) {
            throw new CreditAiProviderException("Le fournisseur IA n’est pas configuré.");
        }
        String factsJson;
        try {
            factsJson = objectMapper.writeValueAsString(facts);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Impossible de préparer les données fictives pour l’analyse IA.", exception);
        }
        GeminiGenerateContentRequest request = new GeminiGenerateContentRequest(
            new SystemInstruction(List.of(new Part(SYSTEM_PROMPT))),
            List.of(new Content("user", List.of(new Part(factsJson)))),
            new GenerationConfig("application/json"));
        try {
            GeminiGenerateContentResponse response = client.post()
                .uri("/models/{model}:generateContent", properties.getModel())
                .header("x-goog-api-key", properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(GeminiGenerateContentResponse.class);
            if (response == null || response.candidates() == null || response.candidates().isEmpty()
                    || response.candidates().get(0).content() == null
                    || response.candidates().get(0).content().parts() == null) {
                throw new CreditAiProviderException("Gemini a renvoyé une réponse vide.");
            }
            return response.candidates().get(0).content().parts().stream()
                .map(Part::text)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining());
        } catch (RestClientResponseException exception) {
            throw new CreditAiProviderException(
                providerErrorMessage(exception),
                exception);
        } catch (RestClientException exception) {
            Throwable cause = exception;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            throw new CreditAiProviderException(
                "Erreur de communication avec Gemini (" + cause.getClass().getSimpleName() + ").",
                exception);
        }
    }

    private String providerErrorMessage(RestClientResponseException exception) {
        String statusMessage = "Gemini a répondu avec le statut HTTP " + exception.getStatusCode().value() + ".";
        try {
            JsonNode message = objectMapper.readTree(exception.getResponseBodyAsString())
                .path("error")
                .path("message");
            if (message.isTextual() && !message.asText().isBlank()) {
                String safeMessage = message.asText()
                    .replace(properties.getApiKey(), "[clé masquée]")
                    .replaceAll("[\\r\\n\\t]+", " ")
                    .trim();
                if (safeMessage.length() > 240) {
                    safeMessage = safeMessage.substring(0, 240) + "…";
                }
                return statusMessage + " Détail Gemini : " + safeMessage;
            }
        } catch (JsonProcessingException ignored) {
            return statusMessage;
        }
        return statusMessage;
    }

    private record GeminiGenerateContentRequest(
        SystemInstruction systemInstruction,
        List<Content> contents,
        GenerationConfig generationConfig
    ) {
    }

    private record SystemInstruction(List<Part> parts) {
    }

    private record Content(String role, List<Part> parts) {
    }

    private record Part(String text) {
    }

    private record GenerationConfig(String responseMimeType) {
    }

    private record GeminiGenerateContentResponse(List<Candidate> candidates) {
    }

    private record Candidate(Content content) {
    }
}
