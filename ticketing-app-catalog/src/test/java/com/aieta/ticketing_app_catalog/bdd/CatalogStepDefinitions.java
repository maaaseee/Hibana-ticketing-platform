package com.aieta.ticketing_app_catalog.bdd;

import com.aieta.ticketing_app_catalog.application.query.get_event_by_id.GetEventByIdQuery;
import com.aieta.ticketing_app_catalog.application.query.get_upcoming_events.GetUpcomingEventsQuery;
import com.aieta.ticketing_app_catalog.common.mediator.Mediator;
import com.aieta.ticketing_app_catalog.domain.entity.EventSummary;
import com.aieta.ticketing_app_catalog.domain.exceptions.EventNotFoundException;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CatalogStepDefinitions {

    @Autowired
    private Mediator mediator;

    private List<EventSummary> retrievedEvents;
    private Exception capturedException;

    @Given("the catalog contains published events with future dates")
    public void catalogContainsPublishedEvents() {
        assertNotNull(mediator);
    }

    @When("the user requests upcoming events with category {string}")
    public void userRequestsUpcomingEventsWithCategory(String category) {
        retrievedEvents = mediator.dispatch(new GetUpcomingEventsQuery(category));
    }

    @Then("the response should contain a non-empty list of events")
    public void responseShouldContainNonEmptyList() {
        assertNotNull(retrievedEvents);
        assertFalse(retrievedEvents.isEmpty(), "Expected at least 1 upcoming event from seed data");
    }

    @And("each returned event should have a valid venue name and positive starting price")
    public void eachReturnedEventShouldHaveVenueAndPrice() {
        for (EventSummary event : retrievedEvents) {
            assertNotNull(event.venue(), "Venue should not be null");
            assertNotNull(event.venue().name(), "Venue name must be present");
            assertNotNull(event.sectors(), "Event sectors list must be present");
            assertFalse(event.sectors().isEmpty(), "Event must have sectors");
            assertTrue(event.sectors().getFirst().basePrice().compareTo(BigDecimal.ZERO) > 0, "Base price must be positive");
        }
    }

    @Given("the catalog does not contain an event with ID {string}")
    public void catalogDoesNotContainEvent(String id) {
        capturedException = null;
    }

    @When("the user requests event details for {string}")
    public void userRequestsEventDetailsFor(String id) {
        try {
            mediator.dispatch(new GetEventByIdQuery(id));
        } catch (Exception ex) {
            capturedException = ex;
        }
    }

    @Then("the system returns a 404 Not Found problem detail")
    public void systemReturnsNotFound() {
        assertNotNull(capturedException, "Expected EventNotFoundException");
        assertInstanceOf(EventNotFoundException.class, capturedException);
    }

    @And("the problem title is {string}")
    public void problemTitleMatches(String title) {
        assertTrue(capturedException.getMessage().contains("Event not found"));
    }
}
