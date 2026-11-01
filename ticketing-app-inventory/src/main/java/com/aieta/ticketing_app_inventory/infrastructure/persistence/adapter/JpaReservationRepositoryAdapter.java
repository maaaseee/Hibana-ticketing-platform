package com.aieta.ticketing_app_inventory.infrastructure.persistence.adapter;

import com.aieta.ticketing_app_inventory.domain.model.ReservationId;
import com.aieta.ticketing_app_inventory.domain.model.Seat;
import com.aieta.ticketing_app_inventory.domain.model.SeatId;
import com.aieta.ticketing_app_inventory.domain.model.TicketReservation;
import com.aieta.ticketing_app_inventory.domain.ports.ReservationRepositoryPort;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.entity.SeatJpaEntity;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.entity.TicketReservationJpaEntity;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.mapper.ReservationMapper;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.mapper.SeatMapper;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.repository.SpringDataOutboxRepository;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.repository.SpringDataReservationRepository;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.repository.SpringDataSeatRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaReservationRepositoryAdapter implements ReservationRepositoryPort {
    private static final Logger log = LoggerFactory.getLogger(JpaReservationRepositoryAdapter.class);

    private final SpringDataReservationRepository reservationRepository;
    private final SpringDataSeatRepository seatRepository;
    private final SpringDataOutboxRepository outboxRepository;
    private final ReservationMapper reservationMapper;
    private final SeatMapper seatMapper;
    private final ObjectMapper objectMapper;

    public JpaReservationRepositoryAdapter(
            SpringDataReservationRepository reservationRepository,
            SpringDataSeatRepository seatRepository,
            SpringDataOutboxRepository outboxRepository,
            ReservationMapper reservationMapper,
            SeatMapper seatMapper,
            ObjectMapper objectMapper
    ) {
        this.reservationRepository = reservationRepository;
        this.seatRepository = seatRepository;
        this.outboxRepository = outboxRepository;
        this.reservationMapper = reservationMapper;
        this.seatMapper = seatMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TicketReservation> findById(ReservationId reservationId) {
        return reservationRepository.findById(reservationId.value())
                .map(r -> reservationMapper.toDomain(r));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TicketReservation> findByEventIdAndSeatId(String eventId, SeatId seatId) {
        return reservationRepository.findFirstByEventIdAndSeatIdOrderByCreatedAtDesc(eventId, seatId.value())
                .map(r -> reservationMapper.toDomain(r));
    }

    @Override
    @Transactional
    public TicketReservation saveWithOutbox(TicketReservation reservation, Seat seat, Object domainEvent) {
        log.debug("Persisting reservation {} and seat {} with outbox event in single transaction",
                reservation.getId().value(), seat.getId().value());

        // 1. Update/Persist Seat
        SeatJpaEntity seatEntity = seatMapper.toEntity(seat);
        seatRepository.save(seatEntity);

        // 2. Persist/Update Reservation
        TicketReservationJpaEntity resEntity = reservationMapper.toEntity(reservation);
        TicketReservationJpaEntity savedResEntity = reservationRepository.save(resEntity);

        // 3. Serialize and persist Outbox Event
        if (domainEvent != null) {
            try {
                String payload = objectMapper.writeValueAsString(domainEvent);
                OutboxEventJpaEntity outboxEntity = new OutboxEventJpaEntity(
                        UUID.randomUUID(),
                        "TicketReservation",
                        reservation.getId().value(),
                        domainEvent.getClass().getSimpleName(),
                        payload
                );
                outboxRepository.save(outboxEntity);
                log.debug("Outbox event saved with ID: {}", outboxEntity.getId());
            } catch (Exception e) {
                log.error("Failed to serialize or save outbox event for reservation {}", reservation.getId().value(), e);
                throw new RuntimeException("Transactional outbox serialization/persistence failure: " + e.getMessage(), e);
            }
        }

        return reservationMapper.toDomain(savedResEntity);
    }

    @Override
    @Transactional
    public TicketReservation save(TicketReservation reservation) {
        TicketReservationJpaEntity resEntity = reservationMapper.toEntity(reservation);
        TicketReservationJpaEntity saved = reservationRepository.save(resEntity);
        return reservationMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public long countBySeatId(String seatId) {
        return reservationRepository.countBySeatId(seatId);
    }
}
