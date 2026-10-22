package com.bna.digitalbanking.notification;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:notification-test;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class NotificationControllerIntegrationTest {
    private static final String APPLICATION = """
        {
          "scenario":"SANS_RACHAT",
          "creditType":"Crédit Direct Dépenses Courantes",
          "requestedAmount":5000,
          "durationMonths":12,
          "averageMonthlySalary":2000,
          "halfRentIncome":0,
          "sedDeductions":100,
          "bnaDeductions":100,
          "bnaNonBuyoutDeductions":0,
          "remainingBuyoutCapital":0
        }
        """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsNotificationOnDraftAndSubmissionButNotOnDraftEdit() throws Exception {
        String firstEmail = "notifications-first-" + System.nanoTime() + "@example.test";
        String firstToken = registerAndGetToken(firstEmail);
        String secondToken = registerAndGetToken("notifications-second-" + System.nanoTime() + "@example.test");
        String applicationId = createDraft(firstToken, APPLICATION);

        mockMvc.perform(get("/api/client/notifications")
                .header("Authorization", "Bearer " + firstToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].type").value("APPLICATION_CREATED"))
            .andExpect(jsonPath("$[0].isRead").value(false))
            .andExpect(jsonPath("$[0].applicationId").value(applicationId))
            .andExpect(jsonPath("$[0].message")
                .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("5000"))));

        mockMvc.perform(put("/api/applications/{id}", applicationId)
                .header("Authorization", "Bearer " + firstToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(APPLICATION))
            .andExpect(status().isOk());
        assertNotificationCount(firstToken, 1);

        mockMvc.perform(post("/api/applications/{id}/submit", applicationId)
                .header("Authorization", "Bearer " + firstToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUBMITTED"));
        assertNotificationCount(firstToken, 2);
        mockMvc.perform(get("/api/client/notifications/unread-count")
                .header("Authorization", "Bearer " + firstToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unreadCount").value(2));

        MvcResult firstNotification = mockMvc.perform(get("/api/client/notifications")
                .header("Authorization", "Bearer " + firstToken))
            .andExpect(status().isOk())
            .andReturn();
        String notificationId = objectMapper.readTree(firstNotification.getResponse().getContentAsString())
            .get(0).get("id").asText();

        mockMvc.perform(patch("/api/client/notifications/{id}/read", notificationId)
                .header("Authorization", "Bearer " + firstToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isRead").value(true))
            .andExpect(jsonPath("$.readAt").isNotEmpty());
        mockMvc.perform(get("/api/client/notifications/unread-count")
                .header("Authorization", "Bearer " + firstToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unreadCount").value(1));
        mockMvc.perform(get("/api/client/notifications")
                .header("Authorization", "Bearer " + firstToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].isRead").value(true))
            .andExpect(jsonPath("$[0].readAt").isNotEmpty());

        mockMvc.perform(get("/api/client/notifications")
                .header("Authorization", "Bearer " + secondToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(patch("/api/client/notifications/{id}/read", notificationId)
                .header("Authorization", "Bearer " + secondToken))
            .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/client/notifications/read-all")
                .header("Authorization", "Bearer " + firstToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.updatedCount").value(1));
        mockMvc.perform(get("/api/client/notifications/unread-count")
                .header("Authorization", "Bearer " + firstToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unreadCount").value(0));

        String renewedToken = loginAndGetToken(firstEmail);
        mockMvc.perform(get("/api/client/notifications")
                .header("Authorization", "Bearer " + renewedToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].isRead").value(true))
            .andExpect(jsonPath("$[1].isRead").value(true));
    }

    @Test
    void emitsNotificationsForApprovedAndRejectedBusinessTransitionsOnly() throws Exception {
        String clientToken = registerAndGetToken("notifications-workflow-" + System.nanoTime() + "@example.test");
        String firstApplicationId = createDraft(clientToken, APPLICATION);
        mockMvc.perform(post("/api/applications/{id}/submit", firstApplicationId)
                .header("Authorization", "Bearer " + clientToken))
            .andExpect(status().isOk());

        String adminToken = registerAdminAndGetToken();
        mockMvc.perform(post("/api/admin/applications/{id}/claim", firstApplicationId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));
        assertNotificationCount(clientToken, 3);
        mockMvc.perform(post("/api/admin/applications/{id}/claim", firstApplicationId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isConflict());
        assertNotificationCount(clientToken, 3);

        mockMvc.perform(post("/api/admin/applications/{id}/request-info", firstApplicationId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"note":"Merci de préciser votre situation professionnelle."}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ADDITIONAL_INFO_REQUIRED"));
        mockMvc.perform(post("/api/applications/{id}/provide-info", firstApplicationId)
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"note":"Je vous transmets le complément demandé."}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUBMITTED"));
        mockMvc.perform(post("/api/admin/applications/{id}/claim", firstApplicationId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        mockMvc.perform(post("/api/admin/applications/{id}/decision", firstApplicationId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"decision":"APPROVED","reason":"Décision favorable."}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));

        String secondApplicationId = createDraft(clientToken, APPLICATION);
        mockMvc.perform(post("/api/applications/{id}/submit", secondApplicationId)
                .header("Authorization", "Bearer " + clientToken))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/applications/{id}/claim", secondApplicationId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/applications/{id}/decision", secondApplicationId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"decision":"REJECTED","reason":"Décision défavorable."}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(get("/api/client/notifications")
                .header("Authorization", "Bearer " + clientToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(11))
            .andExpect(jsonPath("$[0].title").value("Demande refusée"))
            .andExpect(jsonPath("$[0].message")
                .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Décision défavorable"))))
            .andExpect(jsonPath("$[4].title").value("Demande acceptée"))
            .andExpect(jsonPath("$[7].title").value("Complément demandé"));
    }

    @Test
    void failedSubmissionDoesNotEmitStatusChangeNotification() throws Exception {
        String token = registerAndGetToken("notifications-failed-" + System.nanoTime() + "@example.test");
        String invalidForSubmission = APPLICATION.replace("\"requestedAmount\":5000,", "");
        String applicationId = createDraft(token, invalidForSubmission);

        mockMvc.perform(post("/api/applications/{id}/submit", applicationId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isBadRequest());
        assertNotificationCount(token, 1);
    }

    private String createDraft(String token, String payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andReturn();
        JsonNode draft = objectMapper.readTree(result.getResponse().getContentAsString());
        return draft.get("id").asText();
    }

    private String registerAndGetToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullName":"Notification Test Client",
                      "email":"%s",
                      "password":"a-strong-test-password"
                    }
                    """.formatted(email)))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
            .get("accessToken").asText();
    }

    private String registerAdminAndGetToken() throws Exception {
        String email = "notifications-admin-" + System.nanoTime() + "@example.test";
        registerAndGetToken(email);
        jdbcTemplate.update("UPDATE app_users SET role = 'ADMIN' WHERE email = ?", email);
        return loginAndGetToken(email);
    }

    private String loginAndGetToken(String email) throws Exception {
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

    private void assertNotificationCount(String token, int expected) throws Exception {
        mockMvc.perform(get("/api/client/notifications")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(expected));
    }
}
