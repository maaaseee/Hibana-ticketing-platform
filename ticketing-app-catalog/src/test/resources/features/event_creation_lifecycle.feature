# language: en
# ==============================================================================
# BDD Specification: Event Creation and Lifecycle Management
# Bounded Context: Event & Venue Catalog (B2B Administrative Command Side)
# Purpose: Validates that organizers can create new draft events with sectors,
#          ensures schedule and domain invariant enforcement, and verifies
#          cancellation of events.
# ==============================================================================
Feature: Event Creation and Lifecycle Management
  As an event organizer on the Back-Office admin portal
  I want to create events with designated sectors and manage their lifecycle
  So that tickets can be issued and sold to customers safely and consistently

  # ----------------------------------------------------------------------------
  # Rule: Valid event creation returns HTTP 202 Accepted with creation metadata
  # ----------------------------------------------------------------------------
  Scenario: Organizer creates a new stadium event with multiple sectors
    Given an organizer with ID "org_amaryllis_live" and an existing venue "ven_estadio_nacional"
    When the organizer submits a request to create event "rock-stadium-fest" titled "Rock Stadium Festival"
    And the event has a sector "VIP Section" with capacity 500 and price 150.00 "USD"
    And the event has a sector "General Admission" with capacity 2500 and price 50.00 "USD"
    Then the event is accepted with status "DRAFT"
    And the total estimated capacity is calculated as 3000

  # ----------------------------------------------------------------------------
  # Rule: Events cannot have inverted schedules (end date before start date)
  # ----------------------------------------------------------------------------
  Scenario: System rejects event creation when schedule is inverted
    Given an organizer with ID "org_amaryllis_live" and an existing venue "ven_estadio_nacional"
    When the organizer submits an event where end time is before start time
    Then the system rejects the creation due to an invalid schedule invariant

  # ----------------------------------------------------------------------------
  # Rule: An active event can be cancelled, transitioning its status to CANCELLED
  # ----------------------------------------------------------------------------
  Scenario: Organizer cancels an active event preventing future sales
    Given an existing event with slug "indie-live-night"
    When the administrator cancels the event with reason "Artist illness"
    Then the event status transitions to "CANCELLED"
