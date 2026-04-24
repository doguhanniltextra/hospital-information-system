package com.project;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "app.secret=mytestsecretmytestsecretmytestsecretmytestsecret",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
@ActiveProfiles("test")
public class GatewayRoutingTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void routesAreConfigured() {
        StepVerifier.create(routeLocator.getRoutes())
                .expectNextMatches(route -> route.getId().equals("patient-service-route"))
                .expectNextMatches(route -> route.getId().equals("api-docs-patient-route"))
                .expectNextMatches(route -> route.getId().equals("doctor-service-route"))
                .thenConsumeWhile(route -> true)
                .verifyComplete();
    }

    @Test
    void specificRouteDetails() {
        routeLocator.getRoutes()
                .filter(route -> route.getId().equals("patient-service-route"))
                .subscribe(route -> {
                    assertThat(route.getUri().toString()).isEqualTo("http://patient-management:8080");
                    // Predicate check would be more complex as it's a combined predicate
                });
    }
}
