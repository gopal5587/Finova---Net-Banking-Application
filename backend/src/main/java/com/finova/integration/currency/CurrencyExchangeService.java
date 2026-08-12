package com.finova.integration.currency;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.finova.common.exception.BusinessRuleException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

/**
 * Converts amounts between currencies. In {@code sandbox} mode rates are local and
 * deterministic; in live mode a public FX API is called through {@link WebClient}.
 */
@Service
public class CurrencyExchangeService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyExchangeService.class);

    /** Deterministic sandbox mid-market rates quoted against INR. */
    private static final Map<String, BigDecimal> SANDBOX_INR_RATES = Map.of(
            "INR", BigDecimal.ONE,
            "USD", new BigDecimal("0.012"),
            "EUR", new BigDecimal("0.011"),
            "GBP", new BigDecimal("0.0095")
    );

    private final WebClient webClient;
    private final boolean sandbox;

    public CurrencyExchangeService(WebClient.Builder webClientBuilder,
                                   @Value("${finova.integrations.currency.base-url:https://api.exchangerate.host}") String baseUrl,
                                   @Value("${finova.integrations.mode:sandbox}") String mode) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.sandbox = !"live".equalsIgnoreCase(mode);
    }

    @CircuitBreaker(name = "currencyApi", fallbackMethod = "convertFallback")
    @Retry(name = "currencyApi")
    public ConversionResult convert(BigDecimal amount, String from, String to) {
        if (amount == null || amount.signum() < 0) {
            throw new BusinessRuleException("Amount must be non-negative");
        }
        String fromCcy = from.toUpperCase();
        String toCcy = to.toUpperCase();

        BigDecimal rate = sandbox ? sandboxRate(fromCcy, toCcy) : liveRate(fromCcy, toCcy);
        BigDecimal converted = amount.multiply(rate).setScale(2, RoundingMode.HALF_EVEN);
        log.debug("Converted {} {} -> {} {} at rate {}", amount, fromCcy, converted, toCcy, rate);
        return new ConversionResult(fromCcy, toCcy, amount, converted, rate, sandbox ? "sandbox" : "live");
    }

    private BigDecimal sandboxRate(String from, String to) {
        BigDecimal fromInInr = SANDBOX_INR_RATES.get(from);
        BigDecimal toInInr = SANDBOX_INR_RATES.get(to);
        if (fromInInr == null || toInInr == null) {
            throw new BusinessRuleException("Unsupported currency pair: " + from + "/" + to);
        }
        // Both rates are quoted as "1 INR = x CCY", so CCY_from -> CCY_to = to/from.
        return toInInr.divide(fromInInr, 8, RoundingMode.HALF_EVEN);
    }

    private BigDecimal liveRate(String from, String to) {
        // Minimal exchangerate.host-compatible call; sandbox is the default path.
        Map<?, ?> body = webClient.get()
                .uri(uri -> uri.path("/convert")
                        .queryParam("from", from)
                        .queryParam("to", to)
                        .queryParam("amount", 1)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        if (body == null || body.get("result") == null) {
            throw new IllegalStateException("Currency API returned an empty response");
        }
        return new BigDecimal(body.get("result").toString());
    }

    @SuppressWarnings("unused")
    private ConversionResult convertFallback(BigDecimal amount, String from, String to, Throwable ex) {
        log.warn("Currency API unavailable ({}); using sandbox fallback", ex.toString());
        BigDecimal rate = sandboxRate(from.toUpperCase(), to.toUpperCase());
        BigDecimal converted = amount.multiply(rate).setScale(2, RoundingMode.HALF_EVEN);
        return new ConversionResult(from.toUpperCase(), to.toUpperCase(), amount, converted, rate, "fallback");
    }

    public record ConversionResult(
            String from,
            String to,
            BigDecimal amount,
            BigDecimal convertedAmount,
            BigDecimal rate,
            String source
    ) {
    }
}
