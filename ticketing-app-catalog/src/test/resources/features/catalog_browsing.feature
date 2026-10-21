# language: en
# ==============================================================================
# BDD Specification: Public Event Catalog Browsing
# Bounded Context: Event & Venue Catalog (B2C Front-Office Client)
# Purpose: Validates that public users can query upcoming events while drafts
#          and past events are strictly filtered out by the database engine.
# ==============================================================================
Feature: Public Event Catalog Browsing
  As a ticket buyer on the Front-Office web portal
  I want to browse upcoming published events with venue details and pricing
  So that I can select tickets for purchase

  # ----------------------------------------------------------------------------
  # Rule: Only events marked as PUBLISHED and with future start dates appear
  # ----------------------------------------------------------------------------
  Scenario: Public user retrieves upcoming published concerts
    Given the catalog contains published events with future dates
    When the user requests upcoming events with category "CONCERT"
    Then the response should contain a non-empty list of events
    And each returned event should have a valid venue name and positive starting price

  # ----------------------------------------------------------------------------
  # Rule: Searching for a non-existent identifier returns RFC 7807 Not Found
  # ----------------------------------------------------------------------------
  Scenario: Public user requests an event by a non-existent identifier
    Given the catalog does not contain an event with ID "evt_nonexistent_999"
    When the user requests event details for "evt_nonexistent_999"
    Then the system returns a 404 Not Found problem detail
    And the problem title is "Event Not Found"
