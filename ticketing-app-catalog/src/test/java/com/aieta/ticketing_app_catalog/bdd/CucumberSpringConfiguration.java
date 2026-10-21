package com.aieta.ticketing_app_catalog.bdd;

import com.aieta.ticketing_app_catalog.TicketingAppCatalogApplication;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@CucumberContextConfiguration
@SpringBootTest(classes = TicketingAppCatalogApplication.class)
public class CucumberSpringConfiguration {
}
