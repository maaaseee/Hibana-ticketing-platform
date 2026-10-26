package com.aieta.ticketing_app_catalog.infrastructure.database.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "venues")
public class VenueJpaEntity {

    @Id
    @Column(name = "id", length = 60, nullable = false)
    private String id;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "city", length = 100, nullable = false)
    private String city;

    @Column(name = "country", length = 100, nullable = false)
    private String country;

    @Column(name = "address", length = 255, nullable = false)
    private String address;

    @Column(name = "latitude", precision = 9, scale = 6, nullable = false)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6, nullable = false)
    private BigDecimal longitude;

    @Column(name = "seat_map_url", length = 500)
    private String seatMapUrl;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public VenueJpaEntity() {}

    public VenueJpaEntity(String id, String name, String city, String country, String address, BigDecimal latitude, BigDecimal longitude) {
        this(id, name, city, country, address, latitude, longitude, null, null);
    }

    public VenueJpaEntity(String id, String name, String city, String country, String address, BigDecimal latitude, BigDecimal longitude, String seatMapUrl) {
        this(id, name, city, country, address, latitude, longitude, seatMapUrl, null);
    }

    public VenueJpaEntity(String id, String name, String city, String country, String address, BigDecimal latitude, BigDecimal longitude, String seatMapUrl, String imageUrl) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.country = country;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.seatMapUrl = seatMapUrl;
        this.imageUrl = imageUrl;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public String getSeatMapUrl() { return seatMapUrl; }
    public void setSeatMapUrl(String seatMapUrl) { this.seatMapUrl = seatMapUrl; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
