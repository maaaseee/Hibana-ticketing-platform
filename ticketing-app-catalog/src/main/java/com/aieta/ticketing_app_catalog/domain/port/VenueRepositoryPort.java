package com.aieta.ticketing_app_catalog.domain.port;

import com.aieta.ticketing_app_catalog.domain.entity.VenueSummary;
import java.util.List;

public interface VenueRepositoryPort {
    List<VenueSummary> findAll();
}
