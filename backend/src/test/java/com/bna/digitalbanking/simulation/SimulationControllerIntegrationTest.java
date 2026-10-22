package com.bna.digitalbanking.simulation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "JWT_SECRET=simulation-test-jwt-secret-value-at-least-32-bytes",
    "spring.datasource.url=jdbc:h2:mem:simulation-api-test;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SimulationControllerIntegrationTest {
    private static final String CURRENT_EXPENSES = "Crédit Direct Dépenses Courantes";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void allThreeSimulationEndpointsAcceptFrontendPayloadsAndReturnCalculationResults() throws Exception {
        String token = registerAndGetToken("simulations-" + System.nanoTime() + "@example.test");

        mockMvc.perform(post("/api/simulations/sans-rachat")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "salaireMensuelMoyen":2500,
                      "loyerMensuel":400,
                      "retenuesSed":200,
                      "retenuesCreditsBna":250,
                      "typeCredit":"%s",
                      "dureeMois":36
                    }
                    """.formatted(CURRENT_EXPENSES)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statut").value("ELIGIBLE"))
            .andExpect(jsonPath("$.totalRevenuNetMoyen").value(2700.00))
            .andExpect(jsonPath("$.montantMaxCredit").value(32197.60))
            .andExpect(jsonPath("$.mensualite").doesNotExist());

        mockMvc.perform(post("/api/simulations/rachat-interne")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "salaireMensuelMoyen":2500,
                      "loyerMensuel":400,
                      "retenuesSed":200,
                      "retenuesCreditsBnaNonRachete":150,
                      "capitalRestantARacheter":12000,
                      "typeCredit":"%s",
                      "dureeMois":36,
                      "montantCreditDemande":11999.99
                    }
                    """.formatted(CURRENT_EXPENSES)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statut").value("HORS_LIMITES"))
            .andExpect(jsonPath("$.error").value("Le montant demandé doit être supérieur ou égal au capital restant dû"));

        mockMvc.perform(post("/api/simulations/rachat-externe")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "salaireMensuelMoyen":1200,
                      "loyerMensuel":0,
                      "retenuesSed":0,
                      "capitalRestantARacheter":1000,
                      "typeCredit":"%s",
                      "dureeMois":36
                    }
                    """.formatted(CURRENT_EXPENSES)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statut").value("NON_ELIGIBLE"))
            .andExpect(jsonPath("$.error").value("Revenu net moyen inférieur à 1 500 DT"))
            .andExpect(jsonPath("$.montantMaxCredit").doesNotExist());
    }

    @Test
    void rejectsInvalidRequestAtHttpBoundaryAndReturnsExplicitMaximumError() throws Exception {
        String token = registerAndGetToken("simulation-errors-" + System.nanoTime() + "@example.test");

        mockMvc.perform(post("/api/simulations/sans-rachat")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "salaireMensuelMoyen":2500,
                      "loyerMensuel":0,
                      "retenuesSed":0,
                      "retenuesCreditsBna":0,
                      "typeCredit":"%s",
                      "dureeMois":0
                    }
                    """.formatted(CURRENT_EXPENSES)))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/simulations/rachat-externe")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "salaireMensuelMoyen":2500,
                      "loyerMensuel":0,
                      "retenuesSed":0,
                      "capitalRestantARacheter":1000,
                      "typeCredit":"%s",
                      "dureeMois":36,
                      "montantCreditDemande":1000000
                    }
                    """.formatted(CURRENT_EXPENSES)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statut").value("HORS_LIMITES"))
            .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("dépasse le maximum")));
    }

    @Test
    void exposesConfiguredProductRatesAndRestrictsExternalBuyoutProducts() throws Exception {
        String token = registerAndGetToken("simulation-catalogue-" + System.nanoTime() + "@example.test");

        mockMvc.perform(get("/api/simulations/catalogue")
                .header("Authorization", "Bearer " + token)
                .param("scenario", "SANS_RACHAT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$['Crédit Direct Aménagement'].maxMonths").value(60))
            .andExpect(jsonPath("$['Crédit Direct Acquisition Véhicule Neuf'].maxMonths").value(84))
            .andExpect(jsonPath("$['Crédit Direct Acquisition Véhicule Neuf'].annualMarginPercent").value(0.03))
            .andExpect(jsonPath("$['Crédit Direct Acquisition Logement (Durée > 7 ans et ≤ 15 ans)'].annualMarginPercent").value(0.0325));

        mockMvc.perform(post("/api/simulations/sans-rachat")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "salaireMensuelMoyen":2500,
                      "loyerMensuel":0,
                      "retenuesSed":0,
                      "retenuesCreditsBna":0,
                      "typeCredit":"Crédit Direct Aménagement",
                      "dureeMois":61
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statut").value("HORS_LIMITES"))
            .andExpect(jsonPath("$.error").value("Durée max autorisée : 60 mois"));

        mockMvc.perform(get("/api/simulations/catalogue")
                .header("Authorization", "Bearer " + token)
                .param("scenario", "RACHAT_EXTERNE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$['Crédit Direct Dépenses Courantes']").exists())
            .andExpect(jsonPath("$['Crédit Direct Aménagement']").exists())
            .andExpect(jsonPath("$['Crédit Direct Acquisition Véhicule Neuf']").doesNotExist());

        mockMvc.perform(post("/api/simulations/rachat-externe")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "salaireMensuelMoyen":2500,
                      "loyerMensuel":0,
                      "retenuesSed":0,
                      "capitalRestantARacheter":1000,
                      "typeCredit":"Crédit Direct Acquisition Véhicule Neuf",
                      "dureeMois":36
                    }
                    """))
            .andExpect(status().isUnprocessableEntity());
    }

    private String registerAndGetToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullName":"Simulation Test Client",
                      "email":"%s",
                      "password":"a-strong-test-password"
                    }
                    """.formatted(email)))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("accessToken").asText();
    }
}
