package com.aieta.ticketing_app_catalog.bdd;

import com.aieta.ticketing_app_catalog.application.command.cancel_event.CancelEventCommand;
import com.aieta.ticketing_app_catalog.application.command.create_event.CreateEventCommand;
import com.aieta.ticketing_app_catalog.application.command.create_event.EventCreateResult;
import com.aieta.ticketing_app_catalog.common.mediator.Mediator;
import com.aieta.ticketing_app_catalog.domain.entity.Event;
import com.aieta.ticketing_app_catalog.domain.entity.EventStatus;
import com.aieta.ticketing_app_catalog.domain.exceptions.InvalidEventScheduleException;
import com.aieta.ticketing_app_catalog.domain.port.EventWriteRepositoryPort;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class EventCreationStepDefinitions {

    @Autowired
    private Mediator mediator;

    @Autowired
    private EventWriteRepositoryPort repository;

    private String organizerId;
    private String venueId;
    private String slug;
    private String title;
    private final List<CreateEventCommand.SectorInput> sectors = new ArrayList<>();
    private EventCreateResult creationResult;
    private Exception capturedException;
    private String existingEventId;

    @Given("an organizer with ID {string} and an existing venue {string}")
    public void organizerAndVenue(String organizerId, String venueId) {
        this.organizerId = organizerId;
        this.venueId = venueId;
        this.sectors.clear();
        this.capturedException = null;
        this.creationResult = null;
    }

    @When("the organizer submits a request to create event {string} titled {string}")
    public void submitCreateEvent(String slug, String title) {
        this.slug = slug + "-" + UUID.randomUUID().toString().substring(0, 8);
        this.title = title;
    }

    @And("the event has a sector {string} with capacity {int} and price {double} {string}")
    public void addSector(String name, int capacity, double price, String currency) {
        this.sectors.add(new CreateEventCommand.SectorInput(
                name,
                "GENERAL_ADMISSION",
                capacity,
                BigDecimal.valueOf(price),
                BigDecimal.valueOf(5.00),
                currency
        ));
    }

    @Then("the event is accepted with status {string}")
    public void eventAcceptedWithStatus(String expectedStatus) {
        Instant now = Instant.now().plus(30, ChronoUnit.DAYS);
        CreateEventCommand command = new CreateEventCommand(
                this.organizerId,
                this.venueId,
                this.slug,
                this.title,
                "CONCERT",
                List.of("Rock"),
                now,
                now.plus(2, ChronoUnit.HOURS),
                now.plus(5, ChronoUnit.HOURS),
                "UTC",
                "{}",
                "{}",
                this.sectors
        );

        this.creationResult = mediator.dispatch(command);
        assertNotNull(this.creationResult);
        assertEquals(EventStatus.valueOf(expectedStatus), this.creationResult.status());
    }

    @And("the total estimated capacity is calculated as {int}")
    public void totalEstimatedCapacityCalculated(int expectedCapacity) {
        assertEquals(expectedCapacity, this.creationResult.totalEstimatedCapacity());
    }

    @When("the organizer submits an event where end time is before start time")
    public void submitInvertedSchedule() {
        Instant starts = Instant.now().plus(10, ChronoUnit.DAYS);
        Instant ends = starts.minus(2, ChronoUnit.HOURS);
        Instant doors = starts.minus(1, ChronoUnit.HOURS);

        CreateEventCommand command = new CreateEventCommand(
                this.organizerId,
                this.venueId,
                "inverted-event-" + UUID.randomUUID().toString().substring(0, 8),
                "Inverted Event",
                "CONCERT",
                List.of("Rock"),
                doors,
                starts,
                ends,
                "UTC",
                "{}",
                "{}",
                List.of(new CreateEventCommand.SectorInput(
                        "Sector A", "GENERAL_ADMISSION", 100, BigDecimal.TEN, BigDecimal.ONE, "ARS"
                ))
        );

        try {
            mediator.dispatch(command);
        } catch (Exception ex) {
            this.capturedException = ex;
        }
    }

    @Then("the system rejects the creation due to an invalid schedule invariant")
    public void systemRejectsInvalidSchedule() {
        assertNotNull(this.capturedException, "Expected InvalidEventScheduleException");
        assertInstanceOf(InvalidEventScheduleException.class, this.capturedException);
    }

    @Given("an existing event with slug {string}")
    public void existingEventWithSlug(String eventSlug) {
        this.capturedException = null;
        Instant now = Instant.now().plus(40, ChronoUnit.DAYS);
        String uniqueSlug = eventSlug + "-" + UUID.randomUUID().toString().substring(0, 8);

        CreateEventCommand command = new CreateEventCommand(
                "org_amaryllis_live",
                "ven_estadio_nacional",
                uniqueSlug,
                "Indie Live Night",
                "CONCERT",
                List.of("Indie"),
                now,
                now.plus(2, ChronoUnit.HOURS),
                now.plus(5, ChronoUnit.HOURS),
                "UTC",
                "{}",
                "{}",
                List.of(new CreateEventCommand.SectorInput(
                        "General", "GENERAL_ADMISSION", 500, BigDecimal.valueOf(3000), BigDecimal.valueOf(300), "ARS"
                ))
        );
        EventCreateResult res = mediator.dispatch(command);
        this.existingEventId = res.id();
    }

    @When("the administrator cancels the event with reason {string}")
    public void adminCancelsEvent(String reason) {
        mediator.dispatch(new CancelEventCommand(this.existingEventId, reason));
    }

    @Then("the event status transitions to {string}")
    public void eventStatusTransitionsTo(String expectedStatus) {
        Event event = repository.findByIdForUpdate(this.existingEventId).orElseThrow();
        assertEquals(EventStatus.valueOf(expectedStatus), event.getStatus());
    }
}
