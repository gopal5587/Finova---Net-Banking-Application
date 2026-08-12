package com.finova.security;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

/**
 * Validates the {@code Authorization: Bearer <token>} header on each request and, when the
 * token is a valid <em>access</em> token, populates the {@link SecurityContextHolder}.
 *
 * <p>The filter never rejects requests itself; it simply authenticates when possible and
 * defers authorization decisions to the security filter chain. This keeps public endpoints
 * reachable and lets {@code GlobalExceptionHandler} shape 401/403 responses uniformly.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";
    private static final String MDC_USER = "user";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(token, request);
        }
        // Attribute subsequent log lines to the authenticated user, if any. Cleared in finally so the
        // value never leaks onto the next request handled by this (pooled) thread.
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean userTagged = false;
        if (authentication != null && authentication.isAuthenticated()) {
            MDC.put(MDC_USER, authentication.getName());
            userTagged = true;
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (userTagged) {
                MDC.remove(MDC_USER);
            }
        }
    }

    private void authenticate(String token, HttpServletRequest request) {
        try {
            Claims claims = jwtService.parse(token);
            if (!jwtService.isAccessToken(claims)) {
                // A refresh token must never grant API access.
                log.debug("Ignoring non-access token on {}", request.getRequestURI());
                return;
            }
            String username = claims.getSubject();
            String role = jwtService.extractRole(claims);
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

            var authentication = new UsernamePasswordAuthenticationToken(username, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException e) {
            // Invalid token: leave the context unauthenticated; the chain will 401 protected routes.
            log.debug("Token authentication failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (StringUtils.hasText(header) && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length()).trim();
        }
        return null;
    }
}
