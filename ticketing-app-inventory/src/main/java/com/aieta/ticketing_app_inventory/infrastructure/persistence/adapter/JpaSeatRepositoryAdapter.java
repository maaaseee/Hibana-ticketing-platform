package com.aieta.ticketing_app_inventory.infrastructure.persistence.adapter;

import com.aieta.ticketing_app_inventory.domain.model.Seat;
import com.aieta.ticketing_app_inventory.domain.model.SeatId;
import com.aieta.ticketing_app_inventory.domain.ports.SeatRepositoryPort;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.entity.SeatJpaEntity;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.mapper.SeatMapper;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.repository.SpringDataSeatRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class JpaSeatRepositoryAdapter implements SeatRepositoryPort {

    private final SpringDataSeatRepository seatRepository;
    private final SeatMapper seatMapper;

    public JpaSeatRepositoryAdapter(SpringDataSeatRepository seatRepository, SeatMapper seatMapper) {
        this.seatRepository = seatRepository;
        this.seatMapper = seatMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Seat> findById(SeatId seatId) {
        return seatRepository.findById(seatId.value())
                .map(s -> seatMapper.toDomain(s));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Seat> findByEventIdAndSeatId(String eventId, String seatId) {
        return seatRepository.findByEventIdAndId(eventId, seatId)
                .map(s -> seatMapper.toDomain(s));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Seat> findByEventId(String eventId) {
        return seatRepository.findByEventId(eventId).stream()
                .map(s -> seatMapper.toDomain(s))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Seat> findByEventIdAndSectorId(String eventId, String sectorId) {
        return seatRepository.findByEventIdAndSectorId(eventId, sectorId).stream()
                .map(s -> seatMapper.toDomain(s))
                .toList();
    }

    @Override
    @Transactional
    public Seat save(Seat seat) {
        SeatJpaEntity entity = seatMapper.toEntity(seat);
        SeatJpaEntity saved = seatRepository.save(entity);
        return seatMapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void saveAll(List<Seat> seats) {
        List<SeatJpaEntity> entities = seats.stream()
                .map(s -> seatMapper.toEntity(s))
                .toList();
        seatRepository.saveAll(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByEventId(String eventId) {
        return seatRepository.countByEventId(eventId);
    }
}
