package com.bna.digitalbanking.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    "spring.datasource.url=jdbc:h2:mem:identity-test;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class IdentityControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registersClientLogsInAndReadsOwnProfile() throws Exception {
        String email = "client-" + System.nanoTime() + "@example.test";
        String registration = """
            {
              "fullName": "Test Client",
              "email": "%s",
              "password": "a-strong-test-password"
            }
            """.formatted(email);

        MvcResult registrationResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registration))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.role").value("CLIENT"))
            .andReturn();
        String registrationToken = tokenFrom(registrationResult);

        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + registrationToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email));

        String login = """
            {
              "email": "%s",
              "password": "a-strong-test-password"
            }
            """.formatted(email);
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(login))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void rejectsDuplicateEmailAndInvalidCredentials() throws Exception {
        String email = "duplicate-" + System.nanoTime() + "@example.test";
        String registration = """
            {
              "fullName": "Test Client",
              "email": "%s",
              "password": "a-strong-test-password"
            }
            """.formatted(email);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registration))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registration))
            .andExpect(status().isConflict());

        String login = """
            {
              "email": "%s",
              "password": "incorrect-password"
            }
            """.formatted(email);
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(login))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void clientCannotAccessAdminRoutes() throws Exception {
        String registration = """
            {
              "fullName": "Test Client",
              "email": "role-%s@example.test",
              "password": "a-strong-test-password"
            }
            """.formatted(System.nanoTime());
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registration))
            .andExpect(status().isOk())
            .andReturn();

        mockMvc.perform(get("/api/admin/applications")
                .header("Authorization", "Bearer " + tokenFrom(result)))
            .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedClientCanUseInitializedCreditSimulation() throws Exception {
        String registration = """
            {
              "fullName": "Simulation Client",
              "email": "simulation-%s@example.test",
              "password": "a-strong-test-password"
            }
            """.formatted(System.nanoTime());
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registration))
            .andExpect(status().isOk())
            .andReturn();

        String simulation = """
            {
              "salaireMensuelMoyen": 2000,
              "cinquantePourcentLoyers": 0,
              "retenuesSed": 100,
              "retenuesCreditsBna": 100,
              "typeCredit": "Crédit Direct Dépenses Courantes",
              "dureeMois": 12,
              "montantCreditDemande": 5000
            }
            """;
        mockMvc.perform(post("/api/simulations/sans-rachat")
                .header("Authorization", "Bearer " + tokenFrom(result))
                .contentType(MediaType.APPLICATION_JSON)
                .content(simulation))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statut").value("ELIGIBLE"))
            .andExpect(jsonPath("$.montantMaxCredit").isNumber());
    }

    private String tokenFrom(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("accessToken").asText();
    }
}
