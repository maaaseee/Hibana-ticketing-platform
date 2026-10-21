package com.aieta.ticketing_service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Spring Context Boot Smoke Test")
class TicketingServiceApplicationTests {

    @Test
    @DisplayName("Spring ApplicationContext loads cleanly on Java 25")
    void contextLoads() {
        assertTrue(true, "Application context loaded successfully");
    }
}
