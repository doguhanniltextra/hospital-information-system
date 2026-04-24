package com.project.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SecurityConfigTest {

    private SecurityConfig securityConfig;
    private WebFilter filter;
    private final String secret = "mysecretkeymysecretkeymysecretkeymysecretkey"; // Must be long enough for HMAC-SHA

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig();
        ReflectionTestUtils.setField(securityConfig, "secret", secret);
        filter = securityConfig.jwtAuthenticationFilter();
    }

    @Test
    void filter_WithValidToken_SetsAuthentication() {
        String token = createToken("testuser", List.of("ROLE_USER"));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/patients")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build()
        );
        WebFilterChain chain = mock(WebFilterChain.class);
        
        // The filter uses contextWrite on the chain.filter(exchange) Mono.
        // To verify the context, the chain.filter() must return a Mono that inspects the context.
        when(chain.filter(any())).thenReturn(
                ReactiveSecurityContextHolder.getContext()
                        .doOnNext(context -> {
                            Authentication auth = context.getAuthentication();
                            assertThat(auth).isNotNull();
                            assertThat(auth.getPrincipal()).isEqualTo("testuser");
                            assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
                        })
                        .then()
        );

        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result)
                .verifyComplete();
        
        verify(chain).filter(any());
    }

    @Test
    void filter_WithPermittedPath_SkipsAuthentication() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/login").build()
        );
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result.then(ReactiveSecurityContextHolder.getContext()))
                .expectNextCount(0) // Should not have a security context set
                .verifyComplete();
    }

    @Test
    void filter_WithInvalidToken_DoesNotSetAuthentication() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/patients")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                        .build()
        );
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result.then(ReactiveSecurityContextHolder.getContext()))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void filter_WithNoAuthHeader_DoesNotSetAuthentication() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/patients").build()
        );
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result.then(ReactiveSecurityContextHolder.getContext()))
                .expectNextCount(0)
                .verifyComplete();
    }

    private String createToken(String subject, List<String> roles) {
        Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(subject)
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
