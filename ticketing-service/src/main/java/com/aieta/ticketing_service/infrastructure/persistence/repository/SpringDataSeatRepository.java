package com.aieta.ticketing_service.infrastructure.persistence.repository;

import com.aieta.ticketing_service.infrastructure.persistence.entity.SeatJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataSeatRepository extends JpaRepository<SeatJpaEntity, String> {
    Optional<SeatJpaEntity> findByEventIdAndId(String eventId, String id);
    List<SeatJpaEntity> findByEventId(String eventId);
    List<SeatJpaEntity> findByEventIdAndSectorId(String eventId, String sectorId);
    long countByEventId(String eventId);
    long countByEventIdAndStatus(String eventId, String status);
}
