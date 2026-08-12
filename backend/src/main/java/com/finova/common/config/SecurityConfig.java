package com.finova.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Baseline security configuration (Phase 1).
 *
 * <p>At this stage only public infrastructure endpoints exist. JWT authentication,
 * role-based authorization, and the auth filter chain are introduced in Phase 2 and
 * will replace the permissive rule below. The stateless session policy and password
 * encoder defined here are already production-shaped so later phases only add filters.
 */
@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/api/v1/ping",
            "/actuator/health/**",
            "/actuator/prometheus",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Stateless JWT-based API: CSRF tokens are unnecessary and would break clients.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        // TODO(Phase 2): switch remaining matchers to authenticated() once JWT filter lands.
                        .anyRequest().permitAll()
                );
        return http.build();
    }

    /**
     * BCrypt with default strength (10). Chosen over plain hashing to make offline
     * brute-force of leaked hashes computationally expensive.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
