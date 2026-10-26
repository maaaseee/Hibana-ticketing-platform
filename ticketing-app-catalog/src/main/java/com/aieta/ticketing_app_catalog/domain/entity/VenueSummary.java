package com.aieta.ticketing_app_catalog.domain.entity;

import java.math.BigDecimal;

public record VenueSummary(
        String id,
        String name,
        String city,
        String country,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String seatMapUrl,
        String imageUrl
) {
    public VenueSummary(String id, String name, String city, String country, String address, BigDecimal latitude, BigDecimal longitude) {
        this(id, name, city, country, address, latitude, longitude, null, null);
    }

    public VenueSummary(String id, String name, String city, String country, String address, BigDecimal latitude, BigDecimal longitude, String seatMapUrl) {
        this(id, name, city, country, address, latitude, longitude, seatMapUrl, null);
    }
}
