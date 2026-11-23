package com.hospital.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GatewayRoutesTest {

    @Autowired
    RouteLocator routes;

    @Test
    void everyServiceHasARoute() {
        assertThat(routes.getRoutes().map(Route::getId).collectList().block())
                .containsExactlyInAnyOrder("patient-service", "appointment-service",
                        "billing-service", "notification-service");
    }
}
