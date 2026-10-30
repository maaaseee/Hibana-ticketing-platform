package com.aieta.ticketing_app_orders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableFeignClients
public class TicketingAppOrdersApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketingAppOrdersApplication.class, args);
    }
}
