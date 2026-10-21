package com.aieta.ticketing_app_catalog.infrastructure.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "catalog.outbox.relay", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelayConfig {
}
