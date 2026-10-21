package com.aieta.ticketing_service.infrastructure.persistence.repository;

import com.aieta.ticketing_service.infrastructure.persistence.entity.TicketReservationJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SpringDataReservationRepository extends JpaRepository<TicketReservationJpaEntity, String> {
    Optional<TicketReservationJpaEntity> findFirstByEventIdAndSeatIdOrderByCreatedAtDesc(String eventId, String seatId);
    Optional<TicketReservationJpaEntity> findByEventIdAndSeatId(String eventId, String seatId);

    @Query("SELECT r FROM TicketReservationJpaEntity r WHERE r.status = :status AND r.expiresAt < :now")
    List<TicketReservationJpaEntity> findExpiredReservations(
            @Param("status") String status,
            @Param("now") Instant now,
            Pageable pageable
    );

    long countBySeatId(String seatId);
    long countByEventId(String eventId);
}
