package com.bna.digitalbanking.account;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.YearMonth;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    "spring.datasource.url=jdbc:h2:mem:account-test;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AccountControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void clientCanManageFakeBalancesAndUseTransactionsForMonthlyBudget() throws Exception {
        String token = registerAndGetToken("account-" + System.nanoTime() + "@example.test");
        MvcResult accountResult = mockMvc.perform(get("/api/accounts")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].type").value("CURRENT"))
            .andExpect(jsonPath("$[0].openingBalance").value(2000))
            .andExpect(jsonPath("$[1].type").value("SAVINGS"))
            .andExpect(jsonPath("$[1].openingBalance").value(5000))
            .andReturn();
        JsonNode accountList = objectMapper.readTree(accountResult.getResponse().getContentAsString());
        long currentId = accountList.get(0).get("id").asLong();
        long savingsId = accountList.get(1).get("id").asLong();
        String currentDate = LocalDate.now().toString();
        String month = YearMonth.now().toString();

        mockMvc.perform(post("/api/accounts/{id}/transactions", currentId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "type":"INCOME",
                      "category":"SALARY",
                      "amount":2500,
                      "occurredAt":"%s",
                      "description":"Revenu fictif"
                    }
                    """.formatted(currentDate)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type").value("INCOME"));

        mockMvc.perform(post("/api/accounts/{id}/transactions", currentId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "type":"EXPENSE",
                      "category":"FOOD",
                      "amount":120,
                      "occurredAt":"%s",
                      "description":"Courses fictives"
                    }
                    """.formatted(currentDate)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.categoryLabel").value("Alimentation"));

        MvcResult transferResult = mockMvc.perform(post("/api/accounts/transfers")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "sourceAccountId":%d,
                      "destinationAccountId":%d,
                      "amount":500,
                      "occurredAt":"%s",
                      "description":"Virement fictif"
                    }
                    """.formatted(currentId, savingsId, currentDate)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sourceAccount.currentBalance").value(3880))
            .andExpect(jsonPath("$.destinationAccount.currentBalance").value(5500))
            .andExpect(jsonPath("$.outgoing.transferReference").isNotEmpty())
            .andReturn();
        JsonNode transfer = objectMapper.readTree(transferResult.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertEquals(
            transfer.get("outgoing").get("transferReference").asText(),
            transfer.get("incoming").get("transferReference").asText());

        mockMvc.perform(put("/api/budget")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "month":"%s",
                      "category":"FOOD",
                      "monthlyLimit":80
                    }
                    """.formatted(month)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalIncome").value(2500))
            .andExpect(jsonPath("$.totalExpenses").value(120))
            .andExpect(jsonPath("$.categories[0].category").value("FOOD"))
            .andExpect(jsonPath("$.categories[0].monthlyLimit").value(80))
            .andExpect(jsonPath("$.categories[0].spent").value(120))
            .andExpect(jsonPath("$.categories[0].remaining").value(-40))
            .andExpect(jsonPath("$.categories[0].exceeded").value(true));

        mockMvc.perform(get("/api/accounts")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void rejectsOverspendingAndProtectsAccountsFromOtherCustomers() throws Exception {
        String firstToken = registerAndGetToken("account-owner-" + System.nanoTime() + "@example.test");
        String secondToken = registerAndGetToken("account-other-" + System.nanoTime() + "@example.test");
        MvcResult created = mockMvc.perform(get("/api/accounts")
                .header("Authorization", "Bearer " + firstToken))
            .andExpect(status().isOk())
            .andReturn();
        long accountId = objectMapper.readTree(created.getResponse().getContentAsString())
            .get(0).get("id").asLong();

        mockMvc.perform(get("/api/accounts/{id}/transactions", accountId)
                .header("Authorization", "Bearer " + secondToken))
            .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/accounts/{id}/transactions", accountId)
                .header("Authorization", "Bearer " + firstToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "type":"EXPENSE",
                      "category":"FOOD",
                      "amount":3000,
                      "occurredAt":"%s"
                    }
                    """.formatted(LocalDate.now())))
            .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get("/api/accounts")
                .header("Authorization", "Bearer " + secondToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].currentBalance").value(2000));
    }

    @Test
    void protectsAccountAndBudgetApisByRoleAndAuthentication() throws Exception {
        mockMvc.perform(get("/api/accounts"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/budget").param("month", YearMonth.now().toString()))
            .andExpect(status().isUnauthorized());

        String adminEmail = "account-admin-" + System.nanoTime() + "@example.test";
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
        mockMvc.perform(get("/api/accounts")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/budget")
                .param("month", YearMonth.now().toString())
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isForbidden());
    }

    private String registerAndGetToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullName":"Account Test Client",
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
