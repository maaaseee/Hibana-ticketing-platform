package com.aieta.ticketing_app_inventory.infrastructure.schedulers;

import com.aieta.ticketing_app_inventory.domain.enums.ReservationStatus;
import com.aieta.ticketing_app_inventory.domain.enums.SeatStatus;
import com.aieta.ticketing_app_inventory.domain.ports.DistributedLockPort;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.entity.SeatJpaEntity;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.entity.TicketReservationJpaEntity;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.repository.SpringDataReservationRepository;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.repository.SpringDataSeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "ticketing.sweeper.expiration", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReservationExpirationSweeper {
    private static final Logger log = LoggerFactory.getLogger(ReservationExpirationSweeper.class);

    private final SpringDataReservationRepository reservationRepository;
    private final SpringDataSeatRepository seatRepository;
    private final DistributedLockPort distributedLockPort;
    private final int batchSize;

    public ReservationExpirationSweeper(
            SpringDataReservationRepository reservationRepository,
            SpringDataSeatRepository seatRepository,
            DistributedLockPort distributedLockPort,
            @Value("${ticketing.sweeper.expiration.batch-size:100}") int batchSize
    ) {
        this.reservationRepository = reservationRepository;
        this.seatRepository = seatRepository;
        this.distributedLockPort = distributedLockPort;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${ticketing.sweeper.expiration.fixed-delay-ms:5000}")
    @Transactional
    public void sweepExpiredReservations() {
        Instant now = Instant.now();
        List<TicketReservationJpaEntity> expiredList = reservationRepository.findExpiredReservations(
                ReservationStatus.PENDING_PAYMENT.name(),
                now,
                PageRequest.of(0, batchSize)
        );

        if (expiredList == null || expiredList.isEmpty()) {
            return;
        }

        log.debug("Sweeper found {} expired reservations to process", expiredList.size());

        for (TicketReservationJpaEntity reservation : expiredList) {
            try {
                // 1. Transition reservation status to EXPIRED
                reservation.setStatus(ReservationStatus.EXPIRED.name());
                reservationRepository.save(reservation);

                // 2. Reset associated Seat in database back to AVAILABLE if currently HELD
                seatRepository.findById(reservation.getSeatId()).ifPresent(seat -> {
                    if (SeatStatus.HELD.name().equals(seat.getStatus())) {
                        seat.setStatus(SeatStatus.AVAILABLE.name());
                        seat.setUpdatedAt(now);
                        seatRepository.save(seat);
                        log.debug("Sweeper reset seat {} status to AVAILABLE", seat.getId());
                    }
                });

                // 3. Ensure any lingering Redis lock key is purged
                distributedLockPort.forceReleaseSeatLock(reservation.getEventId(), reservation.getSeatId());

                log.info("Sweeper successfully expired reservation {} for seat {}", reservation.getId(), reservation.getSeatId());
            } catch (Exception ex) {
                log.error("Sweeper failed processing expired reservation {}: {}", reservation.getId(), ex.getMessage(), ex);
            }
        }
    }
}
