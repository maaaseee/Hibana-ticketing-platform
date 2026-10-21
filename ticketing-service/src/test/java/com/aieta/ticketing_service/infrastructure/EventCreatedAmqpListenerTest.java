package com.aieta.ticketing_service.infrastructure;

import com.aieta.ticketing_service.application.commands.generate_seats.BatchGenerateSeatsCommand;
import com.aieta.ticketing_service.application.common.mediator.Mediator;
import com.aieta.ticketing_service.infrastructure.config.JacksonConfig;
import com.aieta.ticketing_service.infrastructure.messaging.amqp.EventCreatedAmqpListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Unit Test: EventCreatedAmqpListener Processing")
class EventCreatedAmqpListenerTest {

    @Test
    @DisplayName("Should consume EventCreatedEvent JSON payload and dispatch BatchGenerateSeatsCommand")
    void shouldConsumeEventCreatedJsonAndDispatchCommand() {
        Mediator mediator = mock(Mediator.class);
        JacksonConfig jacksonConfig = new JacksonConfig();
        EventCreatedAmqpListener listener = new EventCreatedAmqpListener(mediator, jacksonConfig.objectMapper());

        String jsonPayload = """
                {
                  "eventId": "evt_coldplay_2026",
                  "venueId": "ven_estadio_nacional",
                  "slug": "coldplay-tour-2026",
                  "title": "Coldplay 2026",
                  "sectors": [
                    {
                      "sectorId": "sec_vip",
                      "name": "Cancha VIP Front",
                      "type": "GENERAL_ADMISSION",
                      "totalCapacity": 500,
                      "basePrice": 180.00,
                      "serviceFee": 27.00,
                      "currency": "USD"
                    }
                  ]
                }
                """;

        when(mediator.dispatch(any(BatchGenerateSeatsCommand.class))).thenReturn(500);

        listener.onEventCreatedMessage(jsonPayload);

        ArgumentCaptor<BatchGenerateSeatsCommand> captor = ArgumentCaptor.forClass(BatchGenerateSeatsCommand.class);
        verify(mediator, times(1)).dispatch(captor.capture());

        BatchGenerateSeatsCommand dispatched = captor.getValue();
        assertEquals("evt_coldplay_2026", dispatched.eventId());
        assertEquals("ven_estadio_nacional", dispatched.venueId());
        assertEquals(1, dispatched.sectors().size());
        assertEquals("sec_vip", dispatched.sectors().get(0).sectorId());
        assertEquals(500, dispatched.sectors().get(0).totalCapacity());
    }
}
