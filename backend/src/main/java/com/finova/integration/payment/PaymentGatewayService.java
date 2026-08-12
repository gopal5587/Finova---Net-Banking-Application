package com.finova.integration.payment;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.finova.common.exception.BusinessRuleException;
import com.finova.transaction.TransactionService;
import com.finova.transaction.dto.MoneyRequest;
import com.finova.transaction.dto.TransactionResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

/**
 * Simulates Stripe / PayPal card top-ups into a Finova account.
 *
 * <p>Sandbox mode always succeeds (deterministic for demos/tests) and then posts a local
 * {@code DEPOSIT}. Live mode is intentionally not wired with real keys; the circuit breaker
 * still protects the call path so switching to a real provider later is a contained change.
 */
@Service
public class PaymentGatewayService {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayService.class);
    private static final Set<String> SUPPORTED = Set.of("STRIPE", "PAYPAL");

    private final TransactionService transactionService;
    private final boolean sandbox;
    private final SecureRandom random = new SecureRandom();

    public PaymentGatewayService(TransactionService transactionService,
                                 @Value("${finova.integrations.mode:sandbox}") String mode) {
        this.transactionService = transactionService;
        this.sandbox = !"live".equalsIgnoreCase(mode);
    }

    @CircuitBreaker(name = "paymentGateway")
    @Retry(name = "paymentGateway")
    public PaymentResult topUp(String username, PaymentRequest request) {
        String provider = request.provider().trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED.contains(provider)) {
            throw new BusinessRuleException("Unsupported payment provider: " + request.provider());
        }

        String externalRef = callProvider(provider, request);
        TransactionResponse deposit = transactionService.deposit(
                username,
                request.accountId(),
                new MoneyRequest(request.amount(), provider + " top-up " + externalRef));

        log.info("{} top-up {} settled as deposit {} for account {}",
                provider, externalRef, deposit.reference(), request.accountId());

        return new PaymentResult(provider, externalRef, "SUCCEEDED",
                request.accountId(), request.amount(), "INR");
    }

    private String callProvider(String provider, PaymentRequest request) {
        if (!sandbox) {
            throw new BusinessRuleException("Live payment providers are not configured in this build");
        }
        // Tiny artificial delay + random suffix to look like a real gateway acknowledgement.
        String suffix = Integer.toHexString(random.nextInt(0xFFFFF));
        return provider.substring(0, 3) + "_SANDBOX_" + suffix;
    }
}
