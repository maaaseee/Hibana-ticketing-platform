package com.aieta.ticketing_app_orders.infrastructure.web;

import com.aieta.ticketing_app_orders.infrastructure.web.dto.CreateOrderRequest;
import com.aieta.ticketing_app_orders.infrastructure.web.dto.ProcessPaymentRequest;
import com.aieta.ticketing_app_orders.infrastructure.web.dto.TopUpWalletRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderWebIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should successfully create order via POST /api/v1/orders")
    void shouldCreateOrder() throws Exception {
        String reservationId = "res-" + UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(reservationId, "evt-100", "seat-100", new BigDecimal("59.00"));

        mockMvc.perform(post("/api/v1/orders")
                        .header("X-User-Id", "web-user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId", notNullValue()))
                .andExpect(jsonPath("$.userId", is("web-user-1")))
                .andExpect(jsonPath("$.status", is("PENDING_PAYMENT")))
                .andExpect(jsonPath("$.totalAmount", is(59.0)));
    }

    @Test
    @DisplayName("Should top up wallet and pay order successfully")
    void shouldTopUpAndPayOrder() throws Exception {
        String userId = "web-user-topup-" + UUID.randomUUID();

        // 1. Top-up wallet with 100 EUR
        TopUpWalletRequest topUp = new TopUpWalletRequest(new BigDecimal("100.00"));
        mockMvc.perform(post("/api/v1/wallets/me/top-up")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(topUp)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(100.0)));

        // 2. Create order for 45 EUR
        String reservationId = "res-" + UUID.randomUUID();
        CreateOrderRequest createReq = new CreateOrderRequest(reservationId, "evt-200", "seat-200", new BigDecimal("45.00"));
        MvcResult orderResult = mockMvc.perform(post("/api/v1/orders")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = orderResult.getResponse().getContentAsString();
        String orderId = objectMapper.readTree(responseBody).get("orderId").asText();

        // 3. Process payment with POCKET_WALLET and buyer info
        ProcessPaymentRequest payReq = new ProcessPaymentRequest(
                com.aieta.ticketing_app_orders.domain.enums.PaymentMethod.POCKET_WALLET,
                "comprador@hibana.tk",
                "Comprador Test",
                "20-30123456-7"
        );
        mockMvc.perform(post("/api/v1/orders/{orderId}/pay", orderId)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAID")))
                .andExpect(jsonPath("$.paymentMethod", is("POCKET_WALLET")));

        // 4. Verify wallet balance decreased to 55 EUR
        mockMvc.perform(get("/api/v1/wallets/me/balance")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(55.0)));

        // 5. Download Invoice PDF for this paid order
        mockMvc.perform(get("/api/v1/orders/{orderId}/invoice", orderId)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", containsString("Factura_Hibana_" + orderId)));

        // 6. Resend confirmation email
        mockMvc.perform(post("/api/v1/orders/{orderId}/resend-confirmation", orderId)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DISPATCHED")));
    }

    @Test
    @DisplayName("Should serve preview invoice PDF and email template")
    void shouldServePreviews() throws Exception {
        // Preview PDF
        mockMvc.perform(get("/api/v1/orders/preview/invoice.pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", containsString("Factura_Hibana_Preview.pdf")));

        // Preview HTML
        mockMvc.perform(get("/api/v1/orders/preview/email.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string(containsString("HIBANA.TK")))
                .andExpect(content().string(containsString("Confirmación de Compra")));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when request body is invalid")
    void shouldReturnBadRequestOnValidationFailure() throws Exception {
        // Missing reservationId, negative total
        String invalidJson = """
                {
                    "reservationId": "",
                    "eventId": "evt-1",
                    "seatId": "seat-1",
                    "totalAmount": -10.00
                }
                """;

        mockMvc.perform(post("/api/v1/orders")
                        .header("X-User-Id", "web-user-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Constraint Violation")))
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
    }
}
