package com.project.patient_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Value("${app.secret:mySecretKeyForJwtTokenWhichMustBeAtLeast256BitsLong}")
    private String secret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
            
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Key signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(signingKey)
                        .setAllowedClockSkewSeconds(300)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String userId = claims.getSubject();
                String username = claims.get("preferred_username", String.class);
                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    List<String> roles = claims.get("roles", List.class);
                    List<SimpleGrantedAuthority> authorities = roles != null ? 
                            roles.stream()
                                  .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                  .collect(Collectors.toList()) 
                            : Collections.emptyList();

                    UsernamePasswordAuthenticationToken authToken = 
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception e) {
                // Ignore invalid tokens
            }
        }
        filterChain.doFilter(request, response);
    }
}
