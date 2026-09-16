package com.project.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

public class RateLimitConfigTest {

    private KeyResolver keyResolver;

    @BeforeEach
    void setUp() {
        RateLimitConfig config = new RateLimitConfig();
        keyResolver = config.userKeyResolver();
    }

    @Test
    void userKeyResolver_WithAuthHeader_ReturnsUserKey() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/patients")
                        .header("X-Auth-User-Id", "usr-12345")
                        .header("X-Forwarded-For", "203.0.113.195")
                        .build()
        );

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("user:usr-12345")
                .verifyComplete();
    }

    @Test
    void userKeyResolver_WithXForwardedFor_ReturnsFirstClientIp() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/login")
                        .header("X-Forwarded-For", "203.0.113.195, 10.0.0.1, 192.168.1.1")
                        .build()
        );

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("ip:203.0.113.195")
                .verifyComplete();
    }

    @Test
    void userKeyResolver_WithXRealIp_ReturnsRealIp() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/login")
                        .header("X-Real-IP", "198.51.100.22")
                        .build()
        );

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("ip:198.51.100.22")
                .verifyComplete();
    }

    @Test
    void userKeyResolver_WithRemoteAddressOnly_ReturnsRemoteIp() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/login")
                        .remoteAddress(new InetSocketAddress("172.16.0.5", 54321))
                        .build()
        );

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("ip:172.16.0.5")
                .verifyComplete();
    }

    @Test
    void userKeyResolver_WithNoHeadersOrRemoteAddress_ReturnsUnknownIp() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/login").build()
        );

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("ip:unknown")
                .verifyComplete();
    }
}
