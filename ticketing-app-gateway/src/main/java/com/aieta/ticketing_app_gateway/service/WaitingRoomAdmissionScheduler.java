package com.aieta.ticketing_app_gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WaitingRoomAdmissionScheduler {

    private static final Logger log = LoggerFactory.getLogger(WaitingRoomAdmissionScheduler.class);

    private final WaitingRoomService waitingRoomService;

    @Value("${waiting-room.batch.size:10}")
    private int batchSize;

    public WaitingRoomAdmissionScheduler(WaitingRoomService waitingRoomService) {
        this.waitingRoomService = waitingRoomService;
    }

    @Scheduled(fixedRateString = "${waiting-room.batch.rate-ms:3000}")
    public void processQueueAdmissions() {
        waitingRoomService.getActiveEvents()
                .flatMap(eventId -> waitingRoomService.admitBatch(eventId, batchSize))
                .subscribe(
                        count -> {
                            if (count > 0) {
                                log.debug("[WAITING-ROOM-SCHEDULER] Admitted {} users in current tick", count);
                            }
                        },
                        error -> log.error("[WAITING-ROOM-SCHEDULER] Error processing admissions: {}", error.getMessage())
                );
    }
}
