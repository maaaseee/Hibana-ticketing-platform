package com.aieta.ticketing_app_inventory.application;

import com.aieta.ticketing_app_inventory.application.commands.generate_seats.BatchGenerateSeatsCommand;
import com.aieta.ticketing_app_inventory.application.commands.generate_seats.BatchGenerateSeatsCommandHandler;
import com.aieta.ticketing_app_inventory.application.commands.generate_seats.SectorDefinition;
import com.aieta.ticketing_app_inventory.domain.enums.SeatStatus;
import com.aieta.ticketing_app_inventory.domain.model.Seat;
import com.aieta.ticketing_app_inventory.domain.ports.SeatRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Unit Test: BatchGenerateSeatsCommandHandler & SectorDefinition validations")
class BatchGenerateSeatsCommandHandlerTest {

    private SeatRepositoryPort seatRepositoryPort;
    private BatchGenerateSeatsCommandHandler handler;

    @BeforeEach
    void setUp() {
        seatRepositoryPort = mock(SeatRepositoryPort.class);
        handler = new BatchGenerateSeatsCommandHandler(seatRepositoryPort);
    }

    @Test
    @DisplayName("Should generate correct number of physical seats with AVAILABLE status in ARS currency")
    void shouldGenerateSeatsAcrossSectors() {
        SectorDefinition gaSector = new SectorDefinition(
                "sec_ga", "General Admission", "GENERAL_ADMISSION", 50,
                new BigDecimal("5000.00"), new BigDecimal("500.00"), "ARS"
        );
        SectorDefinition numberedSector = new SectorDefinition(
                "sec_num", "Numbered Section", "NUMBERED_SEATS", 40,
                new BigDecimal("10000.00"), new BigDecimal("1000.00"), "ARS", 10
        );

        BatchGenerateSeatsCommand command = new BatchGenerateSeatsCommand(
                "evt_test", "ven_test", List.of(gaSector, numberedSector)
        );

        Integer totalGenerated = handler.handle(command);
        assertEquals(90, totalGenerated);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Seat>> captor = ArgumentCaptor.forClass(List.class);
        verify(seatRepositoryPort, times(1)).saveAll(captor.capture());

        List<Seat> savedSeats = captor.getValue();
        assertEquals(90, savedSeats.size());
        assertTrue(savedSeats.stream().allMatch(s -> s.getStatus() == SeatStatus.AVAILABLE));
        assertTrue(savedSeats.stream().allMatch(s -> s.getEventId().equals("evt_test")));
        assertTrue(savedSeats.stream().allMatch(s -> "ARS".equals(s.getPrice().currency())));

        // Verify numbered sector respected custom seatsPerRow (10 seats/row -> R1..R4)
        List<Seat> numberedSeats = savedSeats.stream()
                .filter(s -> "sec_num".equals(s.getSectorId()))
                .toList();
        assertEquals(40, numberedSeats.size());
        assertTrue(numberedSeats.stream().anyMatch(s -> "R4".equals(s.getRowIdentifier())));
    }

    @Test
    @DisplayName("SectorDefinition validation: should reject basePrice <= 0")
    void shouldRejectNonPositiveBasePrice() {
        assertThrows(IllegalArgumentException.class, () -> new SectorDefinition(
                "sec_1", "Platea", "NUMBERED_SEATS", 100,
                BigDecimal.ZERO, new BigDecimal("500.00"), "ARS"
        ));

        assertThrows(IllegalArgumentException.class, () -> new SectorDefinition(
                "sec_1", "Platea", "NUMBERED_SEATS", 100,
                new BigDecimal("-100.00"), new BigDecimal("500.00"), "ARS"
        ));
    }

    @Test
    @DisplayName("SectorDefinition validation: should reject serviceFee <= 0")
    void shouldRejectNonPositiveServiceFee() {
        assertThrows(IllegalArgumentException.class, () -> new SectorDefinition(
                "sec_1", "Platea", "NUMBERED_SEATS", 100,
                new BigDecimal("5000.00"), BigDecimal.ZERO, "ARS"
        ));

        assertThrows(IllegalArgumentException.class, () -> new SectorDefinition(
                "sec_1", "Platea", "NUMBERED_SEATS", 100,
                new BigDecimal("5000.00"), new BigDecimal("-50.00"), "ARS"
        ));
    }

    @Test
    @DisplayName("SectorDefinition validation: should reject foreign non-ARS currencies")
    void shouldRejectNonArsCurrency() {
        assertThrows(IllegalArgumentException.class, () -> new SectorDefinition(
                "sec_1", "Platea", "NUMBERED_SEATS", 100,
                new BigDecimal("5000.00"), new BigDecimal("500.00"), "USD"
        ));

        assertThrows(IllegalArgumentException.class, () -> new SectorDefinition(
                "sec_1", "Platea", "NUMBERED_SEATS", 100,
                new BigDecimal("5000.00"), new BigDecimal("500.00"), "EUR"
        ));

        // Default to ARS if null or blank
        SectorDefinition defaultCurrency = new SectorDefinition(
                "sec_1", "Platea", "NUMBERED_SEATS", 100,
                new BigDecimal("5000.00"), new BigDecimal("500.00"), null
        );
        assertEquals("ARS", defaultCurrency.currency());
    }
}
