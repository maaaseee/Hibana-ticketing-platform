package com.aieta.ticketing_app_catalog.domain.exceptions;

import java.time.Instant;

public class InvalidEventScheduleException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidEventScheduleException(String reason) {
        super(reason);
    }

    public static InvalidEventScheduleException doorsAfterStart(Instant doorsOpenAt, Instant startsAt) {
        return new InvalidEventScheduleException(
                "Doors open time (" + doorsOpenAt + ") cannot be after event start time (" + startsAt + ")"
        );
    }

    public static InvalidEventScheduleException endBeforeStart(Instant startsAt, Instant endsAt) {
        return new InvalidEventScheduleException(
                "Event end time (" + endsAt + ") cannot be before start time (" + startsAt + ")"
        );
    }
}
