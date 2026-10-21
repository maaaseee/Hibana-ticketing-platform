package com.aieta.ticketing_app_catalog.application.command.cancel_event;

import com.aieta.ticketing_app_catalog.common.mediator.Request;

public record CancelEventCommand(
        String eventId,
        String reason
) implements Request<Void> {}
