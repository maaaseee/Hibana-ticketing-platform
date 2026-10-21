package com.aieta.ticketing_app_catalog.domain.entity;

import com.aieta.ticketing_app_catalog.domain.exceptions.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Event {

    private final String id;
    private final String organizerId;
    private final String venueId;
    private final String slug;
    private String title;
    private EventStatus status;
    private String category;
    private List<String> genres;
    private Instant doorsOpenAt;
    private Instant startsAt;
    private Instant endsAt;
    private String timezone;
    private String media;
    private String salesPolicy;
    private final List<EventSector> sectors;
    private long version;
    private final Instant createdAt;
    private Instant updatedAt;

    public Event(String id, String organizerId, String venueId, String slug, String title,
            EventStatus status, String category, List<String> genres,
            Instant doorsOpenAt, Instant startsAt, Instant endsAt, String timezone,
            String media, String salesPolicy, List<EventSector> sectors,
            long version, Instant createdAt, Instant updatedAt) {

        validateIdentifier(id, "Event ID");
        validateIdentifier(organizerId, "Organizer ID");
        validateIdentifier(venueId, "Venue ID");
        validateIdentifier(slug, "Event slug");
        validateTitle(title);
        validateSchedule(doorsOpenAt, startsAt, endsAt);

        this.id = id.trim();
        this.organizerId = organizerId.trim();
        this.venueId = venueId.trim();
        this.slug = slug.trim();
        this.title = title.trim();
        this.status = status != null ? status : EventStatus.DRAFT;
        this.category = category != null && !category.isBlank() ? category.trim() : "GENERAL";
        this.genres = genres != null ? new ArrayList<>(genres) : new ArrayList<>();
        this.doorsOpenAt = doorsOpenAt;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.timezone = timezone != null && !timezone.isBlank() ? timezone.trim() : "UTC";
        this.media = media != null ? media : "{}";
        this.salesPolicy = salesPolicy != null ? salesPolicy : "{}";
        this.sectors = sectors != null ? new ArrayList<>(sectors) : new ArrayList<>();
        this.version = version;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static Event createNew(String id, String organizerId, String venueId, String slug, String title,
            String category, List<String> genres,
            Instant doorsOpenAt, Instant startsAt, Instant endsAt, String timezone,
            String media, String salesPolicy, List<EventSector> sectors) {
        Instant now = Instant.now();
        return new Event(id, organizerId, venueId, slug, title, EventStatus.DRAFT, category, genres,
                doorsOpenAt, startsAt, endsAt, timezone, media, salesPolicy, sectors, 0L, now, now);
    }

    public void publish() {
        if (this.status == EventStatus.CANCELLED) {
            throw new EventAlreadyCancelledException(this.id);
        }
        if (this.sectors.isEmpty()) {
            throw new InvalidEventStateException("Cannot publish an event without configured sectors: " + this.id);
        }
        this.status = EventStatus.PUBLISHED;
        this.updatedAt = Instant.now();
    }

    public void cancel(String reason) {
        if (this.status == EventStatus.CANCELLED) {
            throw new EventAlreadyCancelledException(this.id);
        }
        this.status = EventStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public int calculateTotalCapacity() {
        return this.sectors.stream().mapToInt(eventSector -> eventSector.getTotalCapacity()).sum();
    }

    private void validateIdentifier(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidEventIdentifierException(fieldName + " cannot be null or blank");
        }
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new InvalidEventTitleException();
        }
    }

    private void validateSchedule(Instant doorsOpen, Instant starts, Instant ends) {
        if (doorsOpen == null || starts == null || ends == null) {
            throw new InvalidEventScheduleException("Schedule timestamps cannot be null");
        }
        if (doorsOpen.isAfter(starts)) {
            throw InvalidEventScheduleException.doorsAfterStart(doorsOpen, starts);
        }
        if (starts.isAfter(ends)) {
            throw InvalidEventScheduleException.endBeforeStart(starts, ends);
        }
    }

    public String getId() {
        return id;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public String getVenueId() {
        return venueId;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public EventStatus getStatus() {
        return status;
    }

    public String getCategory() {
        return category;
    }

    public List<String> getGenres() {
        return Collections.unmodifiableList(genres);
    }

    public Instant getDoorsOpenAt() {
        return doorsOpenAt;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getMedia() {
        return media;
    }

    public String getSalesPolicy() {
        return salesPolicy;
    }

    public List<EventSector> getSectors() {
        return Collections.unmodifiableList(sectors);
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
