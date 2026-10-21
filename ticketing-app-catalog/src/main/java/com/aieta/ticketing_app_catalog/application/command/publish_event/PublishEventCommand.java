package com.aieta.ticketing_app_catalog.application.command.publish_event;

import com.aieta.ticketing_app_catalog.common.mediator.Request;

public record PublishEventCommand(
        String eventId
) implements Request<Void> {}
