package com.aieta.ticketing_service.infrastructure.web;

import com.aieta.ticketing_service.application.common.mediator.Mediator;
import com.aieta.ticketing_service.application.queries.get_event_seats.EventSeatsResponse;
import com.aieta.ticketing_service.application.queries.get_event_seats.GetEventSeatsQuery;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events")
public class SeatQueryController {

    private final Mediator mediator;

    public SeatQueryController(Mediator mediator) {
        this.mediator = mediator;
    }

    @GetMapping("/{eventId}/seats")
    public ResponseEntity<EventSeatsResponse> getEventSeats(
            @PathVariable("eventId") String eventId,
            @RequestParam(value = "sectorId", required = false) String sectorId
    ) {
        GetEventSeatsQuery query = new GetEventSeatsQuery(eventId, sectorId);
        EventSeatsResponse response = mediator.dispatch(query);
        return ResponseEntity.ok(response);
    }
}
