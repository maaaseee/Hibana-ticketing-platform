package com.aieta.ticketing_service.infrastructure.persistence.adapter;

import com.aieta.ticketing_service.domain.model.Seat;
import com.aieta.ticketing_service.domain.model.SeatId;
import com.aieta.ticketing_service.domain.ports.SeatRepositoryPort;
import com.aieta.ticketing_service.infrastructure.persistence.entity.SeatJpaEntity;
import com.aieta.ticketing_service.infrastructure.persistence.mapper.SeatMapper;
import com.aieta.ticketing_service.infrastructure.persistence.repository.SpringDataSeatRepository;
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
                .map(seatMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Seat> findByEventIdAndSeatId(String eventId, String seatId) {
        return seatRepository.findByEventIdAndId(eventId, seatId)
                .map(seatMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Seat> findByEventId(String eventId) {
        return seatRepository.findByEventId(eventId).stream()
                .map(seatMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Seat> findByEventIdAndSectorId(String eventId, String sectorId) {
        return seatRepository.findByEventIdAndSectorId(eventId, sectorId).stream()
                .map(seatMapper::toDomain)
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
                .map(seatMapper::toEntity)
                .toList();
        seatRepository.saveAll(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByEventId(String eventId) {
        return seatRepository.countByEventId(eventId);
    }
}
