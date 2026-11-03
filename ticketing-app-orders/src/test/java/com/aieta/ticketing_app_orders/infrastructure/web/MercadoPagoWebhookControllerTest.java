package com.aieta.ticketing_app_orders.infrastructure.web;

import com.aieta.ticketing_app_orders.application.common.mediator.Mediator;
import com.aieta.ticketing_app_orders.infrastructure.mercadopago.MercadoPagoClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MercadoPagoWebhookControllerTest {

    private static final String TEST_SECRET = "test-webhook-secret-key-32-bytes-long!";

    @Mock
    private MercadoPagoClient mercadoPagoClient;

    @Mock
    private Mediator mediator;

    private MercadoPagoWebhookController controller;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        controller = new MercadoPagoWebhookController(mercadoPagoClient, mediator, TEST_SECRET, objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Should reject webhook with 401 Unauthorized when x-signature header is missing")
    void shouldRejectWhenSignatureHeaderMissing() throws Exception {
        mockMvc.perform(post("/api/v1/payments/mercadopago/webhook")
                        .param("id", "12345")
                        .param("topic", "payment"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", containsString("Missing x-signature header")));
    }

    @Test
    @DisplayName("Should reject webhook with 401 Unauthorized when x-signature header is malformed (missing v1)")
    void shouldRejectWhenSignatureMissingV1() throws Exception {
        long nowSeconds = System.currentTimeMillis() / 1000L;
        mockMvc.perform(post("/api/v1/payments/mercadopago/webhook")
                        .header("x-signature", "ts=" + nowSeconds)
                        .param("id", "12345")
                        .param("topic", "payment"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Malformed x-signature header")));
    }

    @Test
    @DisplayName("Should reject webhook with 401 Unauthorized when x-signature header is malformed (missing ts)")
    void shouldRejectWhenSignatureMissingTs() throws Exception {
        mockMvc.perform(post("/api/v1/payments/mercadopago/webhook")
                        .header("x-signature", "v1=somehashvalue")
                        .param("id", "12345")
                        .param("topic", "payment"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Malformed x-signature header")));
    }

    @Test
    @DisplayName("Should reject webhook with 401 Unauthorized when timestamp drift exceeds 5 minutes (in the past)")
    void shouldRejectWhenTimestampDriftExceedsFiveMinutesPast() throws Exception {
        long pastSeconds = (System.currentTimeMillis() - (10 * 60 * 1000L)) / 1000L; // 10 minutes ago
        String manifest = "id:12345;request-id:req-1;ts:" + pastSeconds + ";";
        String signatureHeader = controller.generateSignatureHeader(manifest, pastSeconds);

        mockMvc.perform(post("/api/v1/payments/mercadopago/webhook")
                        .header("x-signature", signatureHeader)
                        .header("x-request-id", "req-1")
                        .param("id", "12345")
                        .param("topic", "payment"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("drift exceeded 5 minutes")));
    }

    @Test
    @DisplayName("Should reject webhook with 401 Unauthorized when timestamp drift exceeds 5 minutes (in the future)")
    void shouldRejectWhenTimestampDriftExceedsFiveMinutesFuture() throws Exception {
        long futureSeconds = (System.currentTimeMillis() + (10 * 60 * 1000L)) / 1000L; // 10 minutes in future
        String manifest = "id:12345;request-id:req-1;ts:" + futureSeconds + ";";
        String signatureHeader = controller.generateSignatureHeader(manifest, futureSeconds);

        mockMvc.perform(post("/api/v1/payments/mercadopago/webhook")
                        .header("x-signature", signatureHeader)
                        .header("x-request-id", "req-1")
                        .param("id", "12345")
                        .param("topic", "payment"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("drift exceeded 5 minutes")));
    }

    @Test
    @DisplayName("Should reject webhook with 401 Unauthorized when HMAC signature hash is invalid or forged")
    void shouldRejectWhenSignatureHashIsForged() throws Exception {
        long nowSeconds = System.currentTimeMillis() / 1000L;
        String forgedSignatureHeader = "ts=" + nowSeconds + ",v1=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

        mockMvc.perform(post("/api/v1/payments/mercadopago/webhook")
                        .header("x-signature", forgedSignatureHeader)
                        .header("x-request-id", "req-1")
                        .param("id", "12345")
                        .param("topic", "payment"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Invalid webhook signature")));
    }

    @Test
    @DisplayName("Should accept and process webhook when signature is valid using official MercadoPago template")
    void shouldAcceptWhenSignatureIsValidOfficialTemplate() throws Exception {
        String paymentId = "mp-pay-9988";
        String requestId = "req-uuid-1234";
        long nowSeconds = System.currentTimeMillis() / 1000L;

        String manifest = "id:" + paymentId + ";request-id:" + requestId + ";ts:" + nowSeconds + ";";
        String signatureHeader = controller.generateSignatureHeader(manifest, nowSeconds);

        when(mercadoPagoClient.isPaymentApproved(eq(paymentId))).thenReturn(true);

        mockMvc.perform(post("/api/v1/payments/mercadopago/webhook")
                        .header("x-signature", signatureHeader)
                        .header("x-request-id", requestId)
                        .param("id", paymentId)
                        .param("topic", "payment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PROCESSED")))
                .andExpect(jsonPath("$.paymentId", is(paymentId)));
    }

    @Test
    @DisplayName("Should accept and process webhook when signature is valid with JSON request body")
    void shouldAcceptWhenSignatureIsValidWithBody() throws Exception {
        String paymentId = "mp-pay-7766";
        String requestId = "req-uuid-5678";
        long nowSeconds = System.currentTimeMillis() / 1000L;
        String body = "{\"action\":\"payment.created\",\"data\":{\"id\":\"" + paymentId + "\"},\"type\":\"payment\"}";

        String manifest = requestId + ":" + nowSeconds + ":" + body;
        String signatureHeader = controller.generateSignatureHeader(manifest, nowSeconds);

        when(mercadoPagoClient.isPaymentApproved(eq(paymentId))).thenReturn(true);

        mockMvc.perform(post("/api/v1/payments/mercadopago/webhook")
                        .header("x-signature", signatureHeader)
                        .header("x-request-id", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PROCESSED")))
                .andExpect(jsonPath("$.paymentId", is(paymentId)));
    }

    @Test
    @DisplayName("Should verify direct signature evaluation logic with valid and invalid inputs")
    void shouldVerifyDirectSignatureEvaluation() {
        long nowSeconds = System.currentTimeMillis() / 1000L;
        String paymentId = "pay-direct-1";
        String reqId = "req-direct-1";
        String manifest = "id:" + paymentId + ";request-id:" + reqId + ";ts:" + nowSeconds + ";";
        String validHmac = MercadoPagoWebhookController.calculateHmacSha256(manifest, TEST_SECRET);

        assertTrue(controller.verifySignature(validHmac, String.valueOf(nowSeconds), reqId, paymentId, null, null));
        assertFalse(controller.verifySignature("wrong-hash", String.valueOf(nowSeconds), reqId, paymentId, null, null));
        assertFalse(controller.verifySignature(validHmac, String.valueOf(nowSeconds), "different-req", paymentId, null, null));
    }

    @Test
    @DisplayName("Should fail fast on startup in production when webhook secret is missing")
    void shouldFailFastInProductionWhenSecretMissing() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () ->
                new MercadoPagoWebhookController(mercadoPagoClient, mediator, "", "prod", objectMapper)
        );
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () ->
                new MercadoPagoWebhookController(mercadoPagoClient, mediator, null, "production", objectMapper)
        );
    }

    @Test
    @DisplayName("Should fail fast on startup in production when default dev secret is used")
    void shouldFailFastInProductionWhenDefaultDevSecretUsed() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () ->
                new MercadoPagoWebhookController(mercadoPagoClient, mediator, MercadoPagoWebhookController.DEFAULT_DEV_WEBHOOK_SECRET, "prod", objectMapper)
        );
    }

    @Test
    @DisplayName("Should initialize successfully in production when genuine custom secret is provided")
    void shouldInitializeInProductionWithCustomSecret() {
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                new MercadoPagoWebhookController(mercadoPagoClient, mediator, "custom-production-secret-key-32-bytes!", "prod", objectMapper)
        );
    }
}
