package com.aieta.ticketing_app_catalog.infrastructure.api.dto;

import com.aieta.ticketing_app_catalog.domain.entity.EventSectorSummary;
import com.aieta.ticketing_app_catalog.domain.entity.EventSummary;
import com.aieta.ticketing_app_catalog.domain.entity.VenueSummary;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Component
public class EventDtoMapper {

    private final ObjectMapper objectMapper;

    public EventDtoMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EventResponse toResponse(EventSummary event) {
        List<SectorResponse> sectorResponses = event.sectors() != null
                ? event.sectors().stream().map(this::mapSectorResponse).toList()
                : Collections.emptyList();

        BigDecimal minPrice = sectorResponses.stream()
                .map(SectorResponse::basePrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        String currency = sectorResponses.isEmpty() ? "ARS" : sectorResponses.getFirst().currency();

        String bannerUrl = extractJsonField(event.mediaJson(), "bannerUrl");
        String posterUrl = extractJsonField(event.mediaJson(), "posterUrl");
        String description = extractJsonField(event.mediaJson(), "description");
        BigDecimal serviceFeePercentage = extractServiceFeePercentage(event.salesPolicyJson());

        return new EventResponse(
                event.id(),
                event.slug(),
                event.title(),
                event.status().name(),
                event.category(),
                event.genres(),
                event.doorsOpenAt(),
                event.startsAt(),
                event.endsAt(),
                event.timezone(),
                event.venue() != null ? event.venue().name() : null,
                event.venue() != null ? event.venue().city() : null,
                event.venue() != null ? event.venue().address() : null,
                event.venue() != null ? event.venue().seatMapUrl() : null,
                event.venue() != null ? event.venue().imageUrl() : null,
                minPrice,
                currency,
                bannerUrl,
                posterUrl,
                description,
                event.salesPolicyJson(),
                sectorResponses,
                serviceFeePercentage
        );
    }

    public SectorResponse mapSectorResponse(EventSectorSummary sector) {
        BigDecimal total = sector.basePrice().add(sector.serviceFee());
        return new SectorResponse(
                sector.sectorId(),
                sector.name(),
                sector.type(),
                sector.totalCapacity(),
                sector.basePrice(),
                sector.serviceFee(),
                total,
                sector.currency()
        );
    }

    public VenueResponse mapVenueResponse(VenueSummary venue) {
        return new VenueResponse(
                venue.id(),
                venue.name(),
                venue.city(),
                venue.country(),
                venue.address(),
                venue.latitude(),
                venue.longitude(),
                venue.seatMapUrl(),
                venue.imageUrl()
        );
    }

    private BigDecimal extractServiceFeePercentage(String salesPolicyJson) {
        if (salesPolicyJson == null || salesPolicyJson.isBlank()) {
            return BigDecimal.valueOf(10.0);
        }
        try {
            JsonNode node = objectMapper.readTree(salesPolicyJson);
            JsonNode field = node.get("serviceFeePercentage");
            if (field != null && field.isNumber()) {
                return BigDecimal.valueOf(field.asDouble());
            }
        } catch (Exception ignored) {
        }
        return BigDecimal.valueOf(10.0);
    }

    private String extractJsonField(String jsonString, String fieldName) {
        if (jsonString == null || jsonString.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(jsonString);
            JsonNode field = node.get(fieldName);
            return field != null ? field.asString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
