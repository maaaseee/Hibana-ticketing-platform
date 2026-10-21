package com.aieta.ticketing_app_catalog.infrastructure.database.repository;

import com.aieta.ticketing_app_catalog.infrastructure.database.entity.VenueJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataVenueRepository extends JpaRepository<VenueJpaEntity, String> {
}
