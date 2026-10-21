package com.aieta.ticketing_app_catalog.infrastructure.database.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "events")
public class EventJpaEntity extends Auditable {

    @Id
    @Column(name = "id", length = 60, nullable = false)
    private String id;

    @Column(name = "organizer_id", length = 60, nullable = false)
    private String organizerId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "venue_id", nullable = false)
    private VenueJpaEntity venue;

    @Column(name = "slug", length = 200, nullable = false, unique = true)
    private String slug;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "category", length = 50, nullable = false)
    private String category;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "genres", columnDefinition = "text[]")
    private String[] genres = new String[0];

    @Column(name = "doors_open_at", nullable = false)
    private Instant doorsOpenAt;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "timezone", length = 50, nullable = false)
    private String timezone = "UTC";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "media", columnDefinition = "jsonb")
    private String media = "{}";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sales_policy", columnDefinition = "jsonb")
    private String salesPolicy = "{}";

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<EventSectorJpaEntity> sectors = new ArrayList<>();

    public EventJpaEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrganizerId() { return organizerId; }
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }
    public VenueJpaEntity getVenue() { return venue; }
    public void setVenue(VenueJpaEntity venue) { this.venue = venue; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String[] getGenres() { return genres; }
    public void setGenres(String[] genres) { this.genres = genres; }
    public Instant getDoorsOpenAt() { return doorsOpenAt; }
    public void setDoorsOpenAt(Instant doorsOpenAt) { this.doorsOpenAt = doorsOpenAt; }
    public Instant getStartsAt() { return startsAt; }
    public void setStartsAt(Instant startsAt) { this.startsAt = startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public void setEndsAt(Instant endsAt) { this.endsAt = endsAt; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public String getMedia() { return media; }
    public void setMedia(String media) { this.media = media; }
    public String getSalesPolicy() { return salesPolicy; }
    public void setSalesPolicy(String salesPolicy) { this.salesPolicy = salesPolicy; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    public List<EventSectorJpaEntity> getSectors() { return sectors; }
    public void setSectors(List<EventSectorJpaEntity> sectors) { this.sectors = sectors; }
}
