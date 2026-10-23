package com.aieta.ticketing_app_inventory.application.common.mediator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class Mediator {
    private static final Logger log = LoggerFactory.getLogger(Mediator.class);
    private final Map<? extends Class<?>, RequestHandler<?, ?>> requestHandlerMap;

    public Mediator(List<RequestHandler<?, ?>> requestHandlers) {
        this.requestHandlerMap = requestHandlers.stream()
                .collect(Collectors.toMap(handler -> handler.getRequestType(), Function.identity()));
    }

    @SuppressWarnings("unchecked")
    public <R, T extends Request<R>> R dispatch(T request) {
        RequestHandler<T, R> handler = (RequestHandler<T, R>) requestHandlerMap.get(request.getClass());
        if (handler == null) {
            log.error("No handler registered for request type: {}", request.getClass().getName());
            throw new HandlerNotFoundException(request.getClass());
        }
        return handler.handle(request);
    }
}
