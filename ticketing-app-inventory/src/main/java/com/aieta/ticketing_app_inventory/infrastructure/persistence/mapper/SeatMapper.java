package com.aieta.ticketing_app_inventory.infrastructure.persistence.mapper;

import com.aieta.ticketing_app_inventory.domain.enums.SeatStatus;
import com.aieta.ticketing_app_inventory.domain.model.Money;
import com.aieta.ticketing_app_inventory.domain.model.Seat;
import com.aieta.ticketing_app_inventory.domain.model.SeatId;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.entity.SeatJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class SeatMapper {

    public Seat toDomain(SeatJpaEntity entity) {
        if (entity == null) return null;
        return Seat.reconstitute(
                SeatId.of(entity.getId()),
                entity.getEventId(),
                entity.getSectorId(),
                entity.getRowIdentifier(),
                entity.getSeatNumber(),
                Money.of(entity.getPrice(), entity.getCurrency()),
                SeatStatus.valueOf(entity.getStatus()),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public SeatJpaEntity toEntity(Seat domain) {
        if (domain == null) return null;
        return new SeatJpaEntity(
                domain.getId().value(),
                domain.getEventId(),
                domain.getSectorId(),
                domain.getRowIdentifier(),
                domain.getSeatNumber(),
                domain.getPrice().amount(),
                domain.getPrice().currency(),
                domain.getStatus().name(),
                domain.getVersion(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
