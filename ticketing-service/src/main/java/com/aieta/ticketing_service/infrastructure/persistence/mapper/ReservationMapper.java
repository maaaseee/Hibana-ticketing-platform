package com.aieta.ticketing_service.infrastructure.persistence.mapper;

import com.aieta.ticketing_service.domain.enums.ReservationStatus;
import com.aieta.ticketing_service.domain.model.ReservationId;
import com.aieta.ticketing_service.domain.model.SeatId;
import com.aieta.ticketing_service.domain.model.TicketReservation;
import com.aieta.ticketing_service.infrastructure.persistence.entity.TicketReservationJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {

    public TicketReservation toDomain(TicketReservationJpaEntity entity) {
        if (entity == null) return null;
        return TicketReservation.reconstitute(
                ReservationId.of(entity.getId()),
                entity.getEventId(),
                SeatId.of(entity.getSeatId()),
                entity.getUserId(),
                ReservationStatus.valueOf(entity.getStatus()),
                entity.getExpiresAt(),
                entity.getCreatedAt(),
                entity.getConfirmedAt(),
                entity.getOrderId()
        );
    }

    public TicketReservationJpaEntity toEntity(TicketReservation domain) {
        if (domain == null) return null;
        return new TicketReservationJpaEntity(
                domain.getId().value(),
                domain.getEventId(),
                domain.getSeatId().value(),
                domain.getUserId(),
                domain.getStatus().name(),
                domain.getExpiresAt(),
                domain.getCreatedAt(),
                domain.getConfirmedAt(),
                domain.getOrderId()
        );
    }
}
