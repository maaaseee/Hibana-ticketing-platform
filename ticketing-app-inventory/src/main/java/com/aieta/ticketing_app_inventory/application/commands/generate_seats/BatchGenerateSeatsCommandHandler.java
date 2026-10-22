package com.aieta.ticketing_app_inventory.application.commands.generate_seats;

import com.aieta.ticketing_app_inventory.application.common.mediator.RequestHandler;
import com.aieta.ticketing_app_inventory.domain.model.Money;
import com.aieta.ticketing_app_inventory.domain.model.Seat;
import com.aieta.ticketing_app_inventory.domain.model.SeatId;
import com.aieta.ticketing_app_inventory.domain.ports.SeatRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class BatchGenerateSeatsCommandHandler implements RequestHandler<BatchGenerateSeatsCommand, Integer> {
    private static final Logger log = LoggerFactory.getLogger(BatchGenerateSeatsCommandHandler.class);

    private final SeatRepositoryPort seatRepositoryPort;

    public BatchGenerateSeatsCommandHandler(SeatRepositoryPort seatRepositoryPort) {
        this.seatRepositoryPort = seatRepositoryPort;
    }

    @Override
    public Integer handle(BatchGenerateSeatsCommand command) {
        log.info("Generating physical seats for event {} across {} sectors", command.eventId(), command.sectors().size());

        List<Seat> allSeats = new ArrayList<>();

        for (SectorDefinition sector : command.sectors()) {
            BigDecimal totalPrice = sector.basePrice().add(sector.serviceFee());
            Money price = Money.of(totalPrice, sector.currency());

            if ("GENERAL_ADMISSION".equalsIgnoreCase(sector.type())) {
                for (int i = 1; i <= sector.totalCapacity(); i++) {
                    String seatIdStr = String.format("seat_%s_%s_%05d", command.eventId(), sector.sectorId(), i);
                    allSeats.add(Seat.createNew(
                            SeatId.of(seatIdStr),
                            command.eventId(),
                            sector.sectorId(),
                            "GA",
                            String.format("GA-%d", i),
                            price
                    ));
                }
            } else {
                // NUMBERED_SEATS: 20 seats per row
                int seatsPerRow = 20;
                int totalRows = (int) Math.ceil((double) sector.totalCapacity() / seatsPerRow);
                int seatCounter = 1;

                for (int r = 1; r <= totalRows && seatCounter <= sector.totalCapacity(); r++) {
                    String rowIdentifier = String.format("R%d", r);
                    for (int s = 1; s <= seatsPerRow && seatCounter <= sector.totalCapacity(); s++) {
                        String seatNumber = String.valueOf(s);
                        String seatIdStr = String.format("seat_%s_%s_%s_%s", command.eventId(), sector.sectorId(), rowIdentifier, seatNumber);
                        allSeats.add(Seat.createNew(
                                SeatId.of(seatIdStr),
                                command.eventId(),
                                sector.sectorId(),
                                rowIdentifier,
                                seatNumber,
                                price
                        ));
                        seatCounter++;
                    }
                }
            }
        }

        log.info("Batch saving {} seats into database for event {}", allSeats.size(), command.eventId());
        seatRepositoryPort.saveAll(allSeats);
        log.info("Successfully persisted {} seats for event {}", allSeats.size(), command.eventId());

        return allSeats.size();
    }

    @Override
    public Class<BatchGenerateSeatsCommand> getRequestType() {
        return BatchGenerateSeatsCommand.class;
    }
}
