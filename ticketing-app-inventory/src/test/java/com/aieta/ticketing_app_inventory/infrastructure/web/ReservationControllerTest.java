package com.aieta.ticketing_app_inventory.infrastructure.web;

import com.aieta.ticketing_app_inventory.application.commands.confirm_reservation.ConfirmReservationCommand;
import com.aieta.ticketing_app_inventory.application.commands.confirm_reservation.ConfirmationResult;
import com.aieta.ticketing_app_inventory.application.commands.hold_seat.HoldSeatCommand;
import com.aieta.ticketing_app_inventory.application.commands.hold_seat.ReservationResult;
import com.aieta.ticketing_app_inventory.application.common.mediator.Mediator;
import com.aieta.ticketing_app_inventory.domain.enums.ReservationStatus;
import com.aieta.ticketing_app_inventory.domain.exceptions.ReservationExpiredException;
import com.aieta.ticketing_app_inventory.domain.exceptions.SeatAlreadyHeldException;
import com.aieta.ticketing_app_inventory.domain.exceptions.SeatNotFoundException;
import com.aieta.ticketing_app_inventory.domain.ports.ReservationRepositoryPort;
import com.aieta.ticketing_app_inventory.infrastructure.web.error.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReservationController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("Web Slice Test: ReservationController & RFC 7807 ProblemDetail")
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Mediator mediator;

    @MockitoBean
    @SuppressWarnings("unused")
    private ReservationRepositoryPort reservationRepositoryPort;

    @Test
    @DisplayName("POST /api/v1/reservations returns 201 Created with reservation details")
    void shouldReturn201WhenHoldSeatSucceeds() throws Exception {
        String jsonRequest = """
                {
                  "eventId": "evt_coldplay_2026",
                  "seatId": "seat_104",
                  "userId": "user_alice"
                }
                """;

        Instant expiresAt = Instant.now().plusSeconds(600);
        ReservationResult result = new ReservationResult(
                "res_abc123",
                "evt_coldplay_2026",
                "seat_104",
                "user_alice",
                ReservationStatus.PENDING_PAYMENT,
                expiresAt,
                600
        );

        when(mediator.dispatch(any(HoldSeatCommand.class))).thenReturn(result);

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value("res_abc123"))
                .andExpect(jsonPath("$.seatId").value("seat_104"))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.countdownSeconds").value(600));
    }

    @Test
    @DisplayName("POST /api/v1/reservations returns 409 Conflict with RFC 7807 when seat already held")
    void shouldReturn409ConflictWhenSeatAlreadyHeld() throws Exception {
        String jsonRequest = """
                {
                  "eventId": "evt_coldplay_2026",
                  "seatId": "seat_104",
                  "userId": "user_bob"
                }
                """;

        when(mediator.dispatch(any(HoldSeatCommand.class)))
                .thenThrow(new SeatAlreadyHeldException("evt_coldplay_2026", "seat_104"));

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Seat Already Held"))
                .andExpect(jsonPath("$.code").value("SEAT_ALREADY_HELD"))
                .andExpect(jsonPath("$.errorCode").value("SEAT_ALREADY_HELD"))
                .andExpect(jsonPath("$.seatId").value("seat_104"));
    }

    @Test
    @DisplayName("POST /api/v1/reservations returns 404 Not Found when seat does not exist")
    void shouldReturn404NotFoundWhenSeatDoesNotExist() throws Exception {
        String jsonRequest = """
                {
                  "eventId": "evt_coldplay_2026",
                  "seatId": "seat_missing",
                  "userId": "user_bob"
                }
                """;

        when(mediator.dispatch(any(HoldSeatCommand.class)))
                .thenThrow(new SeatNotFoundException("evt_coldplay_2026", "seat_missing"));

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SEAT_NOT_FOUND"))
                .andExpect(jsonPath("$.errorCode").value("SEAT_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /api/v1/reservations/{id}/confirm returns 200 OK upon payment approval")
    void shouldReturn200WhenConfirmingReservation() throws Exception {
        ConfirmationResult result = new ConfirmationResult(
                "res_abc123",
                "evt_coldplay_2026",
                "seat_104",
                ReservationStatus.CONFIRMED,
                "ord_999",
                Instant.now()
        );

        when(mediator.dispatch(any(ConfirmReservationCommand.class))).thenReturn(result);

        String jsonRequest = "{\"orderId\": \"ord_999\"}";

        mockMvc.perform(post("/api/v1/reservations/res_abc123/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value("res_abc123"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.orderId").value("ord_999"));
    }

    @Test
    @DisplayName("POST /api/v1/reservations/{id}/confirm returns 410 Gone when reservation expired")
    void shouldReturn410GoneWhenReservationExpired() throws Exception {
        when(mediator.dispatch(any(ConfirmReservationCommand.class)))
                .thenThrow(new ReservationExpiredException("res_abc123", Instant.now().minusSeconds(5)));

        mockMvc.perform(post("/api/v1/reservations/res_abc123/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("RESERVATION_EXPIRED"))
                .andExpect(jsonPath("$.errorCode").value("RESERVATION_EXPIRED"));
    }
}
