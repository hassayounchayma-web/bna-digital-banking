package com.bna.digitalbanking.credit.ai.service;

import java.net.http.HttpClient;
import java.util.List;
import com.bna.digitalbanking.credit.ai.dto.CreditAiFacts;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OpenAiCompatibleProvider implements CreditAiProvider {
    private static final String SYSTEM_PROMPT = """
        Tu aides un évaluateur humain à examiner une demande fictive de crédit.
        Les données reçues sont uniquement des valeurs de démonstration et ne contiennent aucune identité.
        Les champs de stabilité et d’épargne sont dérivés des transactions fictives enregistrées ;
        l’historique de remboursement est synthétique et ne décrit aucun paiement réel.
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

    public OpenAiCompatibleProvider(
            CreditAiProperties properties,
            ObjectMapper objectMapper) {
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
        OpenAiChatRequest request = new OpenAiChatRequest(
            properties.getModel(),
            List.of(
                new OpenAiMessage("system", SYSTEM_PROMPT),
                new OpenAiMessage("user", factsJson)),
            new ResponseFormat("json_object"));
        try {
            OpenAiChatResponse response = client.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(OpenAiChatResponse.class);
            if (response == null || response.choices() == null || response.choices().isEmpty()
                    || response.choices().get(0).message() == null) {
                throw new CreditAiProviderException("Le fournisseur IA a renvoyé une réponse vide.");
            }
            return response.choices().get(0).message().content();
        } catch (RestClientException exception) {
            throw new CreditAiProviderException("Le fournisseur IA est indisponible.", exception);
        }
    }

    private record OpenAiChatRequest(
        String model,
        List<OpenAiMessage> messages,
        @JsonProperty("response_format") ResponseFormat responseFormat
    ) {
    }

    private record OpenAiMessage(String role, String content) {
    }

    private record ResponseFormat(String type) {
    }

    private record OpenAiChatResponse(List<Choice> choices) {
    }

    private record Choice(OpenAiMessage message) {
    }
}
