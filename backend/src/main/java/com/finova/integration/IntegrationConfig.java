package com.finova.integration;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

/**
 * Shared {@link WebClient} for outbound integrations. Timeouts are set at the connector
 * level so a hung third-party cannot stall request threads indefinitely; Resilience4j
 * adds retry/circuit-breaker on top in each client service.
 */
@Configuration
public class IntegrationConfig {

    @Bean
    public WebClient.Builder webClientBuilder(
            @Value("${finova.integrations.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${finova.integrations.read-timeout-ms:3000}") int readTimeoutMs) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(readTimeoutMs));
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
