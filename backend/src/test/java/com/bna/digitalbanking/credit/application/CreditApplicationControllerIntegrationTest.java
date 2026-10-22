package com.bna.digitalbanking.credit.application;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bna.digitalbanking.credit.scoring.service.CreditScoringRefreshRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:application-test;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CreditApplicationControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CreditScoringRefreshRunner scoringRefreshRunner;

    @Test
    void clientAndAdminCanCompleteAuditedHumanReviewWorkflow() throws Exception {
        String email = "application-" + System.nanoTime() + "@example.test";
        String token = registerAndGetToken(email);
        String payload = """
            {
              "scenario": "SANS_RACHAT",
              "creditType": "Crédit Direct Dépenses Courantes",
              "requestedAmount": 5000,
              "durationMonths": 12,
              "averageMonthlySalary": 2000,
              "halfRentIncome": 0,
              "sedDeductions": 100,
              "bnaDeductions": 100,
              "bnaNonBuyoutDeductions": 0,
              "remainingBuyoutCapital": 0
            }
            """;
        MvcResult draftResult = mockMvc.perform(post("/api/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.simulation.maximumAmount").isNumber())
            .andReturn();
        JsonNode draft = objectMapper.readTree(draftResult.getResponse().getContentAsString());
        String id = draft.get("id").asText();

        mockMvc.perform(post("/api/applications/{id}/submit", id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUBMITTED"))
            .andExpect(jsonPath("$.scoring.provisional").value(true))
            .andExpect(jsonPath("$.scoring.factors.length()").value(4))
            .andExpect(jsonPath("$.scoring.missingInformation.length()").value(2))
            .andExpect(jsonPath("$.scoring.factors[2].points").doesNotExist())
            .andExpect(jsonPath("$.scoring.disclaimer").isNotEmpty());

        mockMvc.perform(post("/api/applications/{id}/submit", id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isConflict());

        mockMvc.perform(get("/api/admin/applications")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());

        String adminEmail = "admin-" + System.nanoTime() + "@example.test";
        registerAndGetToken(adminEmail);
        jdbcTemplate.update("UPDATE app_users SET role = 'ADMIN' WHERE email = ?", adminEmail);
        MvcResult adminLogin = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"a-strong-test-password"}
                    """.formatted(adminEmail)))
            .andExpect(status().isOk())
            .andReturn();
        String adminToken = objectMapper.readTree(adminLogin.getResponse().getContentAsString())
            .get("accessToken").asText();

        mockMvc.perform(post("/api/admin/applications/{id}/claim", id)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        mockMvc.perform(post("/api/admin/applications/{id}/request-info", id)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"note":"Merci de préciser votre situation professionnelle."}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ADDITIONAL_INFO_REQUIRED"));

        mockMvc.perform(post("/api/applications/{id}/provide-info", id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"note":"Je suis salarié en contrat à durée indéterminée."}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUBMITTED"));

        mockMvc.perform(post("/api/admin/applications/{id}/claim", id)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        mockMvc.perform(post("/api/admin/applications/{id}/decision", id)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"decision":"REJECTED","reason":"Capacité de remboursement insuffisante."}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.history.length()").value(7))
            .andExpect(jsonPath("$.history[6].actorRole").value("ADMIN"));
    }

    @Test
    void excludesUnverifiedRepaymentHistoryFromScoring() throws Exception {
        String token = registerAndGetToken("scoring-" + System.nanoTime() + "@example.test");
        String payload = """
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
              "latePaymentsLast12Months": 120,
              "savingsRegularityMonthsLast12Months": 6
            }
            """;
        MvcResult created = mockMvc.perform(post("/api/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        MvcResult submitted = mockMvc.perform(post("/api/applications/{id}/submit", id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scoring.provisional").value(true))
            .andExpect(jsonPath("$.scoring.scoreOutOf100").isNumber())
            .andExpect(jsonPath("$.scoring.factors[0].code").value("DEBT"))
            .andExpect(jsonPath("$.scoring.factors[0].maximumPoints").value(25))
            .andExpect(jsonPath("$.scoring.factors[1].maximumPoints").value(20))
            .andExpect(jsonPath("$.scoring.factors[2].maximumPoints").value(34))
            .andExpect(jsonPath("$.scoring.factors[2].points").doesNotExist())
            .andExpect(jsonPath("$.scoring.factors[3].code").value("SAVINGS_REGULARITY"))
            .andExpect(jsonPath("$.scoring.factors[3].maximumPoints").value(21))
            .andExpect(jsonPath("$.scoring.factors[3].points").doesNotExist())
            .andExpect(jsonPath("$.latePaymentsLast12Months").doesNotExist())
            .andExpect(jsonPath("$.scoring.missingInformation.length()").value(2))
            .andReturn();
        JsonNode scoreResult = objectMapper.readTree(submitted.getResponse().getContentAsString())
            .get("scoring");
        BigDecimal awardedPoints = BigDecimal.ZERO;
        BigDecimal maximumPoints = BigDecimal.ZERO;
        BigDecimal totalMaximumPoints = BigDecimal.ZERO;
        for (JsonNode factor : scoreResult.get("factors")) {
            totalMaximumPoints = totalMaximumPoints.add(factor.get("maximumPoints").decimalValue());
            if (factor.get("points").isNumber()) {
                awardedPoints = awardedPoints.add(factor.get("points").decimalValue());
                maximumPoints = maximumPoints.add(factor.get("maximumPoints").decimalValue());
            }
        }
        org.junit.jupiter.api.Assertions.assertEquals(0, new BigDecimal("100").compareTo(totalMaximumPoints));
        BigDecimal expectedScore = awardedPoints.multiply(new BigDecimal("100"))
            .divide(maximumPoints, 2, java.math.RoundingMode.HALF_UP);
        org.junit.jupiter.api.Assertions.assertEquals(
            0, expectedScore.compareTo(scoreResult.get("scoreOutOf100").decimalValue()));

        jdbcTemplate.update(
            "UPDATE credit_scoring_evaluations SET score_out_of_100 = 42 WHERE application_id = ?",
            Long.parseLong(id));
        jdbcTemplate.update("""
            UPDATE credit_scoring_factors
            SET factor_max_points = CASE factor_order
                WHEN 0 THEN 30
                WHEN 1 THEN 25
                WHEN 2 THEN 20
                WHEN 3 THEN 10
                ELSE factor_max_points
            END
            WHERE evaluation_id = (
                SELECT id FROM credit_scoring_evaluations WHERE application_id = ?
            )
            """, Long.parseLong(id));
        org.junit.jupiter.api.Assertions.assertTrue(scoringRefreshRunner.refreshExistingEvaluations() >= 1);

        mockMvc.perform(get("/api/applications/{id}", id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scoring.provisional").value(true))
            .andExpect(jsonPath("$.scoring.scoreOutOf100").value(expectedScore.doubleValue()))
            .andExpect(jsonPath("$.scoring.factors.length()").value(4))
            .andExpect(jsonPath("$.scoring.factors[0].maximumPoints").value(25))
            .andExpect(jsonPath("$.scoring.factors[1].maximumPoints").value(20))
            .andExpect(jsonPath("$.scoring.factors[2].maximumPoints").value(34))
            .andExpect(jsonPath("$.scoring.factors[3].maximumPoints").value(21))
            .andExpect(jsonPath("$.incomeStabilityMonths").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void detectsIncomeStabilityAndSavingsFromRecordedAccountTransactions() throws Exception {
        String token = registerAndGetToken("automatic-evidence-" + System.nanoTime() + "@example.test");
        MvcResult accountsResult = mockMvc.perform(get("/api/accounts")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode accounts = objectMapper.readTree(accountsResult.getResponse().getContentAsString());
        long currentAccountId = 0;
        long savingsAccountId = 0;
        for (JsonNode account : accounts) {
            if ("CURRENT".equals(account.get("type").asText())) {
                currentAccountId = account.get("id").asLong();
            } else if ("SAVINGS".equals(account.get("type").asText())) {
                savingsAccountId = account.get("id").asLong();
            }
        }
        org.junit.jupiter.api.Assertions.assertTrue(currentAccountId > 0);
        org.junit.jupiter.api.Assertions.assertTrue(savingsAccountId > 0);

        YearMonth currentMonth = YearMonth.from(LocalDate.now());
        for (int offset = 0; offset < 3; offset++) {
            LocalDate date = currentMonth.minusMonths(offset).atDay(1);
            mockMvc.perform(post("/api/accounts/{id}/transactions", currentAccountId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"type":"INCOME","category":"SALARY","amount":5000,"occurredAt":"%s"}
                        """.formatted(date)))
                .andExpect(status().isOk());
            mockMvc.perform(post("/api/accounts/transfers")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"sourceAccountId":%d,"destinationAccountId":%d,"amount":50,"occurredAt":"%s"}
                        """.formatted(currentAccountId, savingsAccountId, date)))
                .andExpect(status().isOk());
        }

        MvcResult created = mockMvc.perform(post("/api/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "scenario":"SANS_RACHAT",
                      "creditType":"Crédit Direct Dépenses Courantes",
                      "requestedAmount":5000,
                      "durationMonths":12,
                      "averageMonthlySalary":5000,
                      "halfRentIncome":0,
                      "sedDeductions":100,
                      "bnaDeductions":100,
                      "bnaNonBuyoutDeductions":0,
                      "remainingBuyoutCapital":0,
                      "incomeStabilityMonths":0,
                      "savingsRegularityMonthsLast12Months":0
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(post("/api/applications/{id}/submit", id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.incomeStabilityMonths").value(3))
            .andExpect(jsonPath("$.savingsRegularityMonthsLast12Months").value(3))
            .andExpect(jsonPath("$.scoring.provisional").value(false))
            .andExpect(jsonPath("$.scoring.factors[2].inputValue").value("3 mois consécutifs observés"))
            .andExpect(jsonPath("$.scoring.factors[3].inputValue").value("3 mois avec virement observé sur 12"));
    }

    @Test
    void unauthenticatedClientCannotCreateAnApplication() throws Exception {
        mockMvc.perform(post("/api/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "scenario":"SANS_RACHAT",
                      "creditType":"Crédit Direct Dépenses Courantes",
                      "requestedAmount":5000,
                      "durationMonths":12,
                      "averageMonthlySalary":5000,
                      "halfRentIncome":0,
                      "sedDeductions":0,
                      "bnaDeductions":0,
                      "bnaNonBuyoutDeductions":0,
                      "remainingBuyoutCapital":0
                    }
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void canSubmitValidRequestsForAllThreeScenarios() throws Exception {
        String token = registerAndGetToken("three-scenarios-" + System.nanoTime() + "@example.test");
        for (String scenario : new String[] { "SANS_RACHAT", "RACHAT_INTERNE", "RACHAT_EXTERNE" }) {
            String payload = """
                {
                  "scenario":"%s",
                  "creditType":"Crédit Direct Dépenses Courantes",
                  "requestedAmount":5000,
                  "durationMonths":12,
                  "averageMonthlySalary":5000,
                  "halfRentIncome":0,
                  "sedDeductions":100,
                  "bnaDeductions":100,
                  "bnaNonBuyoutDeductions":100,
                  "remainingBuyoutCapital":2000
                }
                """.formatted(scenario);
            MvcResult created = mockMvc.perform(post("/api/applications")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulation.maximumAmount").isNumber())
                .andReturn();
            String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
            mockMvc.perform(post("/api/applications/{id}/submit", id)
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
        }
    }

    @Test
    void rejectsDurationBeyondConfiguredProductLimit() throws Exception {
        String token = registerAndGetToken("duration-limit-" + System.nanoTime() + "@example.test");
        mockMvc.perform(post("/api/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "scenario":"SANS_RACHAT",
                      "creditType":"Crédit Direct Dépenses Courantes",
                      "requestedAmount":5000,
                      "durationMonths":37,
                      "averageMonthlySalary":5000,
                      "halfRentIncome":0,
                      "sedDeductions":0,
                      "bnaDeductions":0,
                      "bnaNonBuyoutDeductions":0,
                      "remainingBuyoutCapital":0
                    }
                    """))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void refusesToSubmitARequestedAmountAboveCalculatedMaximum() throws Exception {
        String token = registerAndGetToken("amount-limit-" + System.nanoTime() + "@example.test");
        MvcResult created = mockMvc.perform(post("/api/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "scenario":"SANS_RACHAT",
                      "creditType":"Crédit Direct Dépenses Courantes",
                      "requestedAmount":1000000,
                      "durationMonths":12,
                      "averageMonthlySalary":1000,
                      "halfRentIncome":0,
                      "sedDeductions":300,
                      "bnaDeductions":200,
                      "bnaNonBuyoutDeductions":0,
                      "remainingBuyoutCapital":0
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(post("/api/applications/{id}/submit", id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void refusesToSubmitWhenBuyoutCapitalExceedsAvailableCapacity() throws Exception {
        String token = registerAndGetToken("insufficient-capacity-" + System.nanoTime() + "@example.test");
        MvcResult created = mockMvc.perform(post("/api/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "scenario":"RACHAT_INTERNE",
                      "creditType":"Crédit Direct Dépenses Courantes",
                      "requestedAmount":5000,
                      "durationMonths":12,
                      "averageMonthlySalary":1000,
                      "halfRentIncome":0,
                      "sedDeductions":0,
                      "bnaDeductions":0,
                      "bnaNonBuyoutDeductions":0,
                      "remainingBuyoutCapital":1000000
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.simulation.status").value("CAPACITE_INSUFFISANTE"))
            .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(post("/api/applications/{id}/submit", id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void clientCannotReadAnotherClientsApplication() throws Exception {
        String firstToken = registerAndGetToken("first-" + System.nanoTime() + "@example.test");
        String secondToken = registerAndGetToken("second-" + System.nanoTime() + "@example.test");
        MvcResult created = mockMvc.perform(post("/api/applications")
                .header("Authorization", "Bearer " + firstToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "scenario":"SANS_RACHAT",
                      "creditType":"Crédit Direct Dépenses Courantes",
                      "durationMonths":12,
                      "averageMonthlySalary":2000,
                      "halfRentIncome":0,
                      "sedDeductions":0,
                      "bnaDeductions":0,
                      "bnaNonBuyoutDeductions":0,
                      "remainingBuyoutCapital":0
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/applications/{id}", id)
                .header("Authorization", "Bearer " + secondToken))
            .andExpect(status().isNotFound());
    }

    private String registerAndGetToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullName":"Application Test Client",
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
