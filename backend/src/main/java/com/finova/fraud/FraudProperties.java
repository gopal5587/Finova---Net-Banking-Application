package com.finova.fraud;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunable fraud-rule thresholds, bound from {@code finova.fraud.*}. Externalising these means risk
 * appetite can be adjusted per environment without a code change.
 *
 * @param largeAmountThreshold  single-transfer amount at/above which a LARGE_AMOUNT flag is raised
 * @param velocityWindowSeconds sliding window for counting rapid transfers
 * @param velocityMaxTransfers  number of transfers within the window that trips the VELOCITY rule
 * @param oddHoursStart         inclusive start hour (UTC) of the "odd hours" window
 * @param oddHoursEnd           exclusive end hour (UTC) of the "odd hours" window
 */
@ConfigurationProperties(prefix = "finova.fraud")
public record FraudProperties(
        BigDecimal largeAmountThreshold,
        long velocityWindowSeconds,
        long velocityMaxTransfers,
        int oddHoursStart,
        int oddHoursEnd
) {
    public FraudProperties {
        if (largeAmountThreshold == null) {
            largeAmountThreshold = new BigDecimal("100000.00");
        }
        if (velocityWindowSeconds <= 0) {
            velocityWindowSeconds = 60;
        }
        if (velocityMaxTransfers <= 0) {
            velocityMaxTransfers = 5;
        }
    }
}
