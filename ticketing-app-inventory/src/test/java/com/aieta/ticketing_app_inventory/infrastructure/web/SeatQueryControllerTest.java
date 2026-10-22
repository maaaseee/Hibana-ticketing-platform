package com.aieta.ticketing_app_inventory.infrastructure.web;

import com.aieta.ticketing_app_inventory.application.common.mediator.Mediator;
import com.aieta.ticketing_app_inventory.application.queries.get_event_seats.EventSeatsResponse;
import com.aieta.ticketing_app_inventory.application.queries.get_event_seats.GetEventSeatsQuery;
import com.aieta.ticketing_app_inventory.application.queries.get_event_seats.SeatResponseDto;
import com.aieta.ticketing_app_inventory.domain.enums.SeatStatus;
import com.aieta.ticketing_app_inventory.infrastructure.web.error.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SeatQueryController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("Web Slice Test: SeatQueryController")
class SeatQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Mediator mediator;

    @Test
    @DisplayName("GET /api/v1/events/{eventId}/seats returns 200 OK with seat availability inventory")
    void shouldReturn200WithEventSeats() throws Exception {
        SeatResponseDto seat1 = new SeatResponseDto(
                "seat_1", "evt_coldplay", "sec_vip", "Row-A", "1",
                new BigDecimal("150.00"), "USD", SeatStatus.AVAILABLE
        );
        SeatResponseDto seat2 = new SeatResponseDto(
                "seat_2", "evt_coldplay", "sec_vip", "Row-A", "2",
                new BigDecimal("150.00"), "USD", SeatStatus.HELD
        );

        EventSeatsResponse response = new EventSeatsResponse(
                "evt_coldplay", 2, 1, 1, 0, 0, List.of(seat1, seat2)
        );

        when(mediator.dispatch(any(GetEventSeatsQuery.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/events/evt_coldplay/seats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt_coldplay"))
                .andExpect(jsonPath("$.totalSeats").value(2))
                .andExpect(jsonPath("$.availableSeats").value(1))
                .andExpect(jsonPath("$.heldSeats").value(1))
                .andExpect(jsonPath("$.seats[0].seatId").value("seat_1"))
                .andExpect(jsonPath("$.seats[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$.seats[1].seatId").value("seat_2"))
                .andExpect(jsonPath("$.seats[1].status").value("HELD"));
    }
}
