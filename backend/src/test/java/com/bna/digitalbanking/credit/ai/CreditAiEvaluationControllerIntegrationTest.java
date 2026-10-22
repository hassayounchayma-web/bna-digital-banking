package com.bna.digitalbanking.credit.ai;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bna.digitalbanking.credit.ai.dto.CreditAiFacts;
import com.bna.digitalbanking.credit.ai.service.CreditAiProviderException;
import com.bna.digitalbanking.credit.ai.service.CreditAiProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:credit-ai-test;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.credit-ai.enabled=false"
})
class CreditAiEvaluationControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private CreditAiProvider provider;

    @Test
    void keepsJavaAndAiResultsSeparateAndComparesBothRiskAndEligibility() throws Exception {
        String clientEmail = "ai-client-" + System.nanoTime() + "@example.test";
        String clientToken = registerAndGetToken(clientEmail);
        long applicationId = createAndSubmitApplication(clientToken);
        String adminToken = createAdminAndGetToken();
        mockMvc.perform(post("/api/admin/applications/{id}/claim", applicationId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        when(provider.isConfigured()).thenReturn(true);
        when(provider.analyze(any())).thenReturn("""
            {
              "eligibility_status": "INELIGIBLE",
              "risk_level": "HIGH",
              "reasons": ["Proposition indépendante de démonstration"],
              "missing_information": [],
              "warnings": ["Vérification humaine requise"]
            }
            """);

        mockMvc.perform(post("/api/admin/applications/{id}/ai-evaluation", applicationId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiEvaluation.status").value("SUCCESS"))
            .andExpect(jsonPath("$.aiEvaluation.eligibilityStatus").value("INELIGIBLE"))
            .andExpect(jsonPath("$.comparison.javaEligibilityStatus").value("ELIGIBLE"))
            .andExpect(jsonPath("$.comparison.aiEligibilityStatus").value("INELIGIBLE"))
            .andExpect(jsonPath("$.comparison.eligibilityAgreement").value(false))
            .andExpect(jsonPath("$.comparison.javaRiskLevel").value("LOW"))
            .andExpect(jsonPath("$.comparison.aiRiskLevel").value("HIGH"))
            .andExpect(jsonPath("$.comparison.riskAgreement").value(false))
            .andExpect(jsonPath("$.comparison.explanations.length()").value(2));

        mockMvc.perform(get("/api/admin/applications/{id}", applicationId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiEvaluation.reasons[0]").value("Proposition indépendante de démonstration"));

        mockMvc.perform(get("/api/applications/{id}", applicationId)
                .header("Authorization", "Bearer " + clientToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiEvaluation").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.comparison").value(org.hamcrest.Matchers.nullValue()));

        ArgumentCaptor<CreditAiFacts> factsCaptor = ArgumentCaptor.forClass(CreditAiFacts.class);
        verify(provider).analyze(factsCaptor.capture());
        String serializedFacts = objectMapper.writeValueAsString(factsCaptor.getValue());
        JsonNode facts = objectMapper.readTree(serializedFacts);
        org.junit.jupiter.api.Assertions.assertFalse(facts.has("name"));
        org.junit.jupiter.api.Assertions.assertFalse(facts.has("email"));
        org.junit.jupiter.api.Assertions.assertFalse(facts.has("reference"));
        org.junit.jupiter.api.Assertions.assertFalse(facts.has("cin"));
        org.junit.jupiter.api.Assertions.assertFalse(facts.has("accountNumber"));
        org.junit.jupiter.api.Assertions.assertFalse(facts.has("customerId"));
        org.junit.jupiter.api.Assertions.assertFalse(facts.has("userId"));
        org.junit.jupiter.api.Assertions.assertFalse(serializedFacts.contains(clientEmail));
    }

    @Test
    void reportsUnconfiguredAndInvalidAiResponsesWithoutReplacingJavaScoring() throws Exception {
        String clientToken = registerAndGetToken("ai-unavailable-" + System.nanoTime() + "@example.test");
        long applicationId = createAndSubmitApplication(clientToken);
        String adminToken = createAdminAndGetToken();
        mockMvc.perform(post("/api/admin/applications/{id}/claim", applicationId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        when(provider.isConfigured()).thenReturn(false);
        mockMvc.perform(post("/api/admin/applications/{id}/ai-evaluation", applicationId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiEvaluation.status").value("UNAVAILABLE"))
            .andExpect(jsonPath("$.aiEvaluation.eligibilityStatus").value("INCONCLUSIVE"))
            .andExpect(jsonPath("$.comparison.eligibilityAgreement").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.comparison.riskAgreement").value(org.hamcrest.Matchers.nullValue()));

        when(provider.isConfigured()).thenReturn(true);
        doThrow(new CreditAiProviderException("Le fournisseur IA a répondu avec le statut HTTP 401."))
            .when(provider).analyze(any());
        mockMvc.perform(post("/api/admin/applications/{id}/ai-evaluation", applicationId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiEvaluation.status").value("UNAVAILABLE"))
            .andExpect(jsonPath("$.aiEvaluation.message")
                .value(org.hamcrest.Matchers.containsString("HTTP 401")));

        doReturn("{not-json").when(provider).analyze(any());
        mockMvc.perform(post("/api/admin/applications/{id}/ai-evaluation", applicationId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiEvaluation.status").value("INVALID_RESPONSE"))
            .andExpect(jsonPath("$.comparison.aiRiskLevel").value("UNKNOWN"));

        mockMvc.perform(get("/api/admin/applications/{id}", applicationId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scoring.scoreOutOf100").isNumber())
            .andExpect(jsonPath("$.aiEvaluation.status").value("INVALID_RESPONSE"));
    }

    private long createAndSubmitApplication(String clientToken) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/applications")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "scenario": "SANS_RACHAT",
                      "creditType": "Crédit Direct Dépenses Courantes",
                      "requestedAmount": 5000,
                      "durationMonths": 12,
                      "averageMonthlySalary": 5000,
                      "halfRentIncome": 0,
                      "sedDeductions": 100,
                      "bnaDeductions": 100,
                      "bnaNonBuyoutDeductions": 0,
                      "remainingBuyoutCapital": 0,
                      "incomeStabilityMonths": 12,
                      "savingsRegularityMonthsLast12Months": 6
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(post("/api/applications/{id}/submit", id)
                .header("Authorization", "Bearer " + clientToken))
            .andExpect(status().isOk());
        return id;
    }

    private String createAdminAndGetToken() throws Exception {
        String email = "ai-admin-" + System.nanoTime() + "@example.test";
        registerAndGetToken(email);
        jdbcTemplate.update("UPDATE app_users SET role = 'ADMIN' WHERE email = ?", email);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"a-strong-test-password"}
                    """.formatted(email)))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
            .get("accessToken").asText();
    }

    private String registerAndGetToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullName":"AI Test Client",
                      "email":"%s",
                      "password":"a-strong-test-password"
                    }
                    """.formatted(email)))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
            .get("accessToken").asText();
    }
}
