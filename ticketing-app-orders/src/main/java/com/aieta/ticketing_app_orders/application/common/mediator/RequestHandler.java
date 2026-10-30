package com.aieta.ticketing_app_orders.application.common.mediator;

public interface RequestHandler<T extends Request<R>, R> {
    R handle(T request);
    Class<T> getRequestType();
}
