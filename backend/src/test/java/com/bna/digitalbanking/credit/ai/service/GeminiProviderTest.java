package com.bna.digitalbanking.credit.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import com.bna.digitalbanking.credit.ai.dto.CreditAiFacts;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

class GeminiProviderTest {
    @Test
    void sendsNativeGeminiGenerateContentRequest() throws IOException {
        AtomicReference<String> requestPath = new AtomicReference<>();
        AtomicReference<String> requestApiKey = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestPath.set(exchange.getRequestURI().getPath());
            requestApiKey.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                {"candidates":[{"content":{"parts":[{"text":"{\\"eligibility_status\\":\\"INCONCLUSIVE\\",\\"risk_level\\":\\"UNKNOWN\\",\\"reasons\\":[],\\"missing_information\\":[],\\"warnings\\":[]}"}]}}]}
                """.strip().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            CreditAiProperties properties = new CreditAiProperties();
            properties.setEnabled(true);
            properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1beta");
            properties.setApiKey("test-gemini-key");
            properties.setModel("gemini-2.5-flash");
            properties.setConnectTimeout(Duration.ofSeconds(2));
            properties.setReadTimeout(Duration.ofSeconds(2));
            ObjectMapper objectMapper = new ObjectMapper();
            GeminiProvider provider = new GeminiProvider(properties, objectMapper);

            String result = provider.analyze(new CreditAiFacts(
                "SANS_RACHAT",
                new BigDecimal("10000"),
                60,
                new BigDecimal("3000"),
                new BigDecimal("20"),
                new BigDecimal("900"),
                new BigDecimal("250"),
                null,
                12,
                6));

            assertEquals(
                "/v1beta/models/gemini-2.5-flash:generateContent",
                requestPath.get());
            assertEquals("test-gemini-key", requestApiKey.get());
            JsonNode request = objectMapper.readTree(requestBody.get());
            assertTrue(request.path("systemInstruction").path("parts").get(0).path("text").asText()
                .contains("évaluateur humain"));
            assertEquals("SANS_RACHAT", objectMapper.readTree(
                request.path("contents").get(0).path("parts").get(0).path("text").asText()).path("scenario").asText());
            assertEquals("application/json", request.path("generationConfig").path("responseMimeType").asText());
            assertTrue(result.startsWith("{\"eligibility_status\":\"INCONCLUSIVE\""));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void reportsGeminiErrorDetailsWithoutLeakingApiKey() throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                exchange.getRequestBody().readAllBytes();
                byte[] response = """
                    {"error":{"code":404,"message":"models/gemini-3.8-flash is not found for API version v1beta"}}
                    """.strip().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(404, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            server.start();
            try {
                CreditAiProperties properties = new CreditAiProperties();
                properties.setEnabled(true);
                properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1beta");
                properties.setApiKey("secret-test-key");
                properties.setModel("gemini-3.8-flash");
                properties.setConnectTimeout(Duration.ofSeconds(2));
                properties.setReadTimeout(Duration.ofSeconds(2));
                GeminiProvider provider = new GeminiProvider(properties, new ObjectMapper());

                CreditAiProviderException exception = assertThrows(
                    CreditAiProviderException.class,
                    () -> provider.analyze(new CreditAiFacts(
                        "SANS_RACHAT",
                        BigDecimal.ONE,
                        1,
                        BigDecimal.ONE,
                        BigDecimal.ZERO,
                        BigDecimal.ONE,
                        BigDecimal.ONE,
                        null,
                        1,
                        1)));

                assertTrue(exception.getMessage().contains("HTTP 404"));
                assertTrue(exception.getMessage().contains("models/gemini-3.8-flash is not found"));
                assertTrue(!exception.getMessage().contains("secret-test-key"));
            } finally {
                server.stop(0);
            }
        }
}
