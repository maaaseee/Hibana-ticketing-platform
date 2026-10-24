package com.aieta.ticketing_app_catalog.application.query.get_catalog_metadata;

import com.aieta.ticketing_app_catalog.common.mediator.RequestHandler;
import com.aieta.ticketing_app_catalog.domain.entity.CatalogMetadata;
import com.aieta.ticketing_app_catalog.domain.entity.VenueSummary;
import com.aieta.ticketing_app_catalog.domain.port.EventRepositoryPort;
import com.aieta.ticketing_app_catalog.domain.port.VenueRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class GetCatalogMetadataQueryHandler implements RequestHandler<GetCatalogMetadataQuery, CatalogMetadata> {

    private final EventRepositoryPort eventRepositoryPort;
    private final VenueRepositoryPort venueRepositoryPort;

    public GetCatalogMetadataQueryHandler(EventRepositoryPort eventRepositoryPort, VenueRepositoryPort venueRepositoryPort) {
        this.eventRepositoryPort = eventRepositoryPort;
        this.venueRepositoryPort = venueRepositoryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogMetadata handle(GetCatalogMetadataQuery query) {
        Instant furthestDate = eventRepositoryPort.findFurthestPublishedEventDate().orElse(null);
        List<VenueSummary> venues = venueRepositoryPort.findAll();
        return new CatalogMetadata(furthestDate, venues);
    }

    @Override
    public Class<GetCatalogMetadataQuery> getRequestType() {
        return GetCatalogMetadataQuery.class;
    }
}
