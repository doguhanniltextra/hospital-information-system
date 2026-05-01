package com.project.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.WebFilter;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@EnableWebFluxSecurity
@Configuration
@EnableConfigurationProperties
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${app.secret}")
    private String secret;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/auth/**", "/api/auth/**", "/actuator/**").permitAll()
                        .pathMatchers("/api/notifications/**").hasAuthority("ROLE_ADMIN")
                        .anyExchange().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    // Disable auto-configuration of UserDetailsService since we're using JWT
    @Bean
    public ReactiveUserDetailsService reactiveUserDetailsService() {
        return username -> Mono.empty();
    }

    @Bean
    public WebFilter jwtAuthenticationFilter() {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();

            log.debug("JWT Filter — path: {}", path);

            // Skip authentication for permitted paths
            if (path.startsWith("/auth/") || path.startsWith("/api/auth/") || path.startsWith("/actuator/")) {
                log.debug("Skipping authentication for permitted path");
                return chain.filter(exchange);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                try {
                    Key signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

                    Claims claims = Jwts.parserBuilder()
                            .setSigningKey(signingKey)
                            .setAllowedClockSkewSeconds(30) // Zero Trust: 30 saniye tolerance
                            .build()
                            .parseClaimsJws(token)
                            .getBody();

                    String subject = claims.getSubject();

                    if (subject != null) {
                        List<String> roles = claims.get("roles", List.class);
                        Collection<SimpleGrantedAuthority> authorities = Collections.emptyList();
                        if (roles != null) {
                            authorities = roles.stream()
                                    .map(SimpleGrantedAuthority::new)
                                    .collect(Collectors.toList());
                        }

                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(subject, token, authorities);

                        log.debug("JWT authentication successful for user: {}", subject);

                        // Forward caller identity to downstream services as trusted internal headers
                        String rolesHeader = (roles != null) ? String.join(",", roles) : "";
                        ServerHttpRequest mutatedRequest = request.mutate()
                                .header("X-Auth-User-Id", subject)
                                .header("X-Auth-Roles", rolesHeader)
                                .build();

                        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
                    }

                } catch (Exception e) {
                    log.warn("JWT parsing failed: {}", e.getMessage());
                }
            } else {
                log.debug("No valid Authorization header found for path: {}", path);
            }

            log.debug("No authentication set — will result in 401");
            return chain.filter(exchange);
        };
    }
}