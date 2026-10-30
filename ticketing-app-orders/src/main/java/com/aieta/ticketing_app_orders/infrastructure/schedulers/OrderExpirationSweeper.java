package com.aieta.ticketing_app_orders.infrastructure.schedulers;

import com.aieta.ticketing_app_orders.application.commands.cancel_order.CancelOrderCommand;
import com.aieta.ticketing_app_orders.application.common.mediator.Mediator;
import com.aieta.ticketing_app_orders.infrastructure.persistence.entity.OrderJpaEntity;
import com.aieta.ticketing_app_orders.infrastructure.persistence.repository.SpringDataOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "ticketing.sweeper", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OrderExpirationSweeper {
    private static final Logger log = LoggerFactory.getLogger(OrderExpirationSweeper.class);

    private final SpringDataOrderRepository orderRepository;
    private final Mediator mediator;

    public OrderExpirationSweeper(SpringDataOrderRepository orderRepository, Mediator mediator) {
        this.orderRepository = orderRepository;
        this.mediator = mediator;
    }

    @Scheduled(fixedDelayString = "${ticketing.sweeper.fixed-delay-ms:30000}")
    @Transactional
    public void sweepExpiredOrders() {
        Instant cutoff = Instant.now().minus(10, ChronoUnit.MINUTES);
        List<OrderJpaEntity> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore("PENDING_PAYMENT", cutoff);

        if (expiredOrders.isEmpty()) {
            return;
        }

        log.info("Sweeper found {} expired PENDING_PAYMENT orders older than 10m", expiredOrders.size());

        for (OrderJpaEntity order : expiredOrders) {
            try {
                mediator.dispatch(new CancelOrderCommand(
                        order.getId().toString(),
                        "SYSTEM",
                        "Order hold window expired after 10 minutes without payment"
                ));
                log.info("Automatically cancelled expired order {}", order.getId());
            } catch (Exception e) {
                log.error("Failed to cancel expired order {}: {}", order.getId(), e.getMessage(), e);
            }
        }
    }
}
