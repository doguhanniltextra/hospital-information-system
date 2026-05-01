package com.project.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    /**
     * Rate limit key strategy (Zero Trust — gateway doesn't forward identity):
     *
     *  1. Authenticated requests → "user:<X-User-Id>"  (per-user bucket)
     *     Each user gets their own independent limit regardless of IP.
     *
     *  2. Anonymous / auth endpoints → "ip:<remote-address>"  (per-IP bucket)
     *     Protects against brute-force on /api/auth/** before a token is issued.
     */
    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId);
            }
            return Mono.justOrEmpty(exchange.getRequest().getRemoteAddress())
                    .map(addr -> "ip:" + addr.getAddress().getHostAddress())
                    .defaultIfEmpty("ip:unknown");
        };
    }
}
