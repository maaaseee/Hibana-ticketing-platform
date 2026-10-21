package com.aieta.ticketing_app_catalog.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@DisplayName("Integration Test: EventController HTTP Endpoints")
class EventControllerIT {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("Should successfully retrieve upcoming events via GET /api/v1/events without reflection parameter errors")
    void shouldGetUpcomingEventsWithoutParameterError() throws Exception {
        mockMvc.perform(get("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should successfully filter upcoming events by category via GET /api/v1/events?category=CONCERT")
    void shouldFilterUpcomingEventsByCategory() throws Exception {
        mockMvc.perform(get("/api/v1/events")
                        .param("category", "CONCERT")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should successfully retrieve event by ID via GET /api/v1/events/{id}")
    void shouldGetEventById() throws Exception {
        mockMvc.perform(get("/api/v1/events/evt_coldplay_2026")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("evt_coldplay_2026"))
                .andExpect(jsonPath("$.slug").value("coldplay-music-of-the-spheres-2026"))
                .andExpect(jsonPath("$.seatMapUrl").value("http://localhost:4577/devstoreaccount1/venues/estadio-nacional-seatmap.json"));
    }
}
