package com.aieta.ticketing_app_inventory.infrastructure.web;

import com.aieta.ticketing_app_inventory.application.commands.confirm_reservation.ConfirmReservationCommand;
import com.aieta.ticketing_app_inventory.application.commands.confirm_reservation.ConfirmationResult;
import com.aieta.ticketing_app_inventory.application.commands.hold_seat.HoldSeatCommand;
import com.aieta.ticketing_app_inventory.application.commands.hold_seat.ReservationResult;
import com.aieta.ticketing_app_inventory.application.commands.release_seat.ReleaseSeatCommand;
import com.aieta.ticketing_app_inventory.application.common.mediator.Mediator;
import com.aieta.ticketing_app_inventory.domain.enums.ReservationStatus;
import com.aieta.ticketing_app_inventory.domain.model.ReservationId;
import com.aieta.ticketing_app_inventory.domain.model.SeatId;
import com.aieta.ticketing_app_inventory.domain.model.TicketReservation;
import com.aieta.ticketing_app_inventory.domain.ports.ReservationRepositoryPort;
import com.aieta.ticketing_app_inventory.infrastructure.security.GatewayStampValidator;
import com.aieta.ticketing_app_inventory.infrastructure.security.SecurityConfig;
import com.aieta.ticketing_app_inventory.infrastructure.web.error.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReservationController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration.class
        })
@Import({SecurityConfig.class, GatewayStampValidator.class, GlobalExceptionHandler.class})
@DisplayName("Security & Access Control Tests: ReservationController & Gateway Stamp Isolation")
class ReservationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GatewayStampValidator stampValidator;

    @MockitoBean
    private Mediator mediator;

    @MockitoBean
    private ReservationRepositoryPort reservationRepositoryPort;

    // --- Task 2: Isolation of /confirm ---

    @Test
    @DisplayName("POST /confirm without authentication or Gateway Stamp returns 401 Unauthorized")
    void confirmReservation_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/reservations/res_123/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": \"ord_123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /confirm called with ROLE_USER returns 403 Forbidden")
    void confirmReservation_withRoleUser_returns403() throws Exception {
        String userId = "user_regular";
        String roles = "ROLE_USER";
        long now = System.currentTimeMillis();
        String sig = stampValidator.generateSignature(userId, roles, now);

        mockMvc.perform(post("/api/v1/reservations/res_123/confirm")
                        .header("X-User-Id", userId)
                        .header("X-User-Roles", roles)
                        .header("X-Gateway-Timestamp", String.valueOf(now))
                        .header("X-Gateway-Signature", sig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": \"ord_123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /confirm called by internal service with ROLE_SERVICE returns 200 OK")
    void confirmReservation_withRoleService_returns200() throws Exception {
        ConfirmationResult result = new ConfirmationResult(
                "res_123",
                "evt_1",
                "seat_1",
                ReservationStatus.CONFIRMED,
                "ord_123",
                Instant.now()
        );
        when(mediator.dispatch(any(ConfirmReservationCommand.class))).thenReturn(result);

        String userId = "orders-service";
        String roles = "ROLE_SERVICE";
        long now = System.currentTimeMillis();
        String sig = stampValidator.generateSignature(userId, roles, now);

        mockMvc.perform(post("/api/v1/reservations/res_123/confirm")
                        .header("X-User-Id", userId)
                        .header("X-User-Roles", roles)
                        .header("X-Gateway-Timestamp", String.valueOf(now))
                        .header("X-Gateway-Signature", sig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": \"ord_123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.orderId").value("ord_123"));
    }

    @Test
    @DisplayName("POST /confirm with spoofed unverified ROLE_SERVICE header returns 401 Unauthorized")
    void confirmReservation_withSpoofedRoleService_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/reservations/res_123/confirm")
                        .header("X-User-Id", "attacker")
                        .header("X-User-Roles", "ROLE_SERVICE")
                        // No signature or invalid signature!
                        .header("X-Gateway-Signature", "forged_invalid_signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": \"ord_123\"}"))
                .andExpect(status().isUnauthorized());
    }

    // --- Task 3: Access control & ownership in DELETE reservations ---

    @Test
    @DisplayName("DELETE /reservations/{id} without authentication returns 401 Unauthorized")
    void releaseReservation_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/reservations/res_alice_1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /reservations/{id} by non-owner user returns 403 Forbidden")
    void releaseReservation_byNonOwner_returns403() throws Exception {
        TicketReservation reservation = TicketReservation.createNew(
                "evt_1",
                SeatId.of("seat_1"),
                "user_alice",
                Duration.ofMinutes(10)
        );
        when(reservationRepositoryPort.findById(any())).thenReturn(Optional.of(reservation));

        String userId = "user_bob";
        String roles = "ROLE_USER";
        long now = System.currentTimeMillis();
        String sig = stampValidator.generateSignature(userId, roles, now);

        mockMvc.perform(delete("/api/v1/reservations/res_alice_1")
                        .header("X-User-Id", userId)
                        .header("X-User-Roles", roles)
                        .header("X-Gateway-Timestamp", String.valueOf(now))
                        .header("X-Gateway-Signature", sig))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("DELETE /reservations/{id} by legitimate owner returns 204 No Content")
    void releaseReservation_byOwner_returns204() throws Exception {
        TicketReservation reservation = TicketReservation.createNew(
                "evt_1",
                SeatId.of("seat_1"),
                "user_alice",
                Duration.ofMinutes(10)
        );
        when(reservationRepositoryPort.findById(any())).thenReturn(Optional.of(reservation));

        String userId = "user_alice";
        String roles = "ROLE_USER";
        long now = System.currentTimeMillis();
        String sig = stampValidator.generateSignature(userId, roles, now);

        mockMvc.perform(delete("/api/v1/reservations/res_alice_1")
                        .header("X-User-Id", userId)
                        .header("X-User-Roles", roles)
                        .header("X-Gateway-Timestamp", String.valueOf(now))
                        .header("X-Gateway-Signature", sig))
                .andExpect(status().isNoContent());

        verify(mediator, times(1)).dispatch(any(ReleaseSeatCommand.class));
    }

    @Test
    @DisplayName("DELETE /reservations/{id} by ROLE_ADMIN returns 204 No Content even for another user's reservation")
    void releaseReservation_byAdmin_returns204() throws Exception {
        TicketReservation reservation = TicketReservation.createNew(
                "evt_1",
                SeatId.of("seat_1"),
                "user_alice",
                Duration.ofMinutes(10)
        );
        when(reservationRepositoryPort.findById(any())).thenReturn(Optional.of(reservation));

        String userId = "admin_super";
        String roles = "ROLE_ADMIN";
        long now = System.currentTimeMillis();
        String sig = stampValidator.generateSignature(userId, roles, now);

        mockMvc.perform(delete("/api/v1/reservations/res_alice_1")
                        .header("X-User-Id", userId)
                        .header("X-User-Roles", roles)
                        .header("X-Gateway-Timestamp", String.valueOf(now))
                        .header("X-Gateway-Signature", sig))
                .andExpect(status().isNoContent());

        verify(mediator, times(1)).dispatch(any(ReleaseSeatCommand.class));
    }

    // --- Task 3: holdSeat userId sourcing from authenticated principal ---

    @Test
    @DisplayName("POST /reservations holdSeat: authenticated principal overrides untrusted body userId")
    void holdSeat_authenticatedPrincipalOverridesBodyUserId() throws Exception {
        String jsonRequest = """
                {
                  "eventId": "evt_coldplay",
                  "seatId": "seat_99",
                  "userId": "attacker_spoofed_user"
                }
                """;

        Instant expiresAt = Instant.now().plusSeconds(600);
        ReservationResult result = new ReservationResult(
                "res_999",
                "evt_coldplay",
                "seat_99",
                "user_genuine_alice",
                ReservationStatus.PENDING_PAYMENT,
                expiresAt,
                600
        );

        when(mediator.dispatch(any(HoldSeatCommand.class))).thenReturn(result);

        String userId = "user_genuine_alice";
        String roles = "ROLE_USER";
        long now = System.currentTimeMillis();
        String sig = stampValidator.generateSignature(userId, roles, now);

        mockMvc.perform(post("/api/v1/reservations")
                        .header("X-User-Id", userId)
                        .header("X-User-Roles", roles)
                        .header("X-Gateway-Timestamp", String.valueOf(now))
                        .header("X-Gateway-Signature", sig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated());

        ArgumentCaptor<HoldSeatCommand> captor = ArgumentCaptor.forClass(HoldSeatCommand.class);
        verify(mediator).dispatch(captor.capture());

        // Verify that the command was issued for user_genuine_alice, NOT attacker_spoofed_user
        assertThat(captor.getValue().userId()).isEqualTo("user_genuine_alice");
    }
}
