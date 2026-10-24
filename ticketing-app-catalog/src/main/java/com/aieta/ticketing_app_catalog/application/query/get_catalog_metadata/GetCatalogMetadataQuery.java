package com.aieta.ticketing_app_catalog.application.query.get_catalog_metadata;

import com.aieta.ticketing_app_catalog.common.mediator.Request;
import com.aieta.ticketing_app_catalog.domain.entity.CatalogMetadata;

public record GetCatalogMetadataQuery() implements Request<CatalogMetadata> {
}
