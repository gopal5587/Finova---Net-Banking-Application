package com.finova.integration.ledger;

import java.time.Instant;

/**
 * One block in the simulated ledger chain. {@code previousHash} links to the prior block;
 * {@code hash} is SHA-256 over the block contents so tampering breaks the chain.
 */
public record LedgerBlock(
        long index,
        Instant timestamp,
        String transactionReference,
        String payload,
        String previousHash,
        String hash
) {
}
