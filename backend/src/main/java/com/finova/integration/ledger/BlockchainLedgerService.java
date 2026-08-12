package com.finova.integration.ledger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * In-memory blockchain-style ledger simulation (Hyperledger-inspired concept only).
 *
 * <p>Each financial movement can optionally be mirrored as a hash-linked block. This is NOT a
 * production DLT - it demonstrates the idea of an immutable, tamper-evident append-only chain
 * that interviewers often ask about in fintech contexts.
 */
@Service
public class BlockchainLedgerService {

    private static final Logger log = LoggerFactory.getLogger(BlockchainLedgerService.class);

    private final List<LedgerBlock> chain = Collections.synchronizedList(new ArrayList<>());

    public BlockchainLedgerService() {
        LedgerBlock genesis = new LedgerBlock(0, Instant.EPOCH, "GENESIS", "genesis", "0", hash(0, Instant.EPOCH, "GENESIS", "genesis", "0"));
        chain.add(genesis);
    }

    public LedgerBlock append(String transactionReference, String payload) {
        synchronized (chain) {
            LedgerBlock previous = chain.get(chain.size() - 1);
            Instant now = Instant.now();
            long index = previous.index() + 1;
            String hash = hash(index, now, transactionReference, payload, previous.hash());
            LedgerBlock block = new LedgerBlock(index, now, transactionReference, payload, previous.hash(), hash);
            chain.add(block);
            log.debug("Appended ledger block #{} for {}", index, transactionReference);
            return block;
        }
    }

    public List<LedgerBlock> snapshot() {
        synchronized (chain) {
            return List.copyOf(chain);
        }
    }

    public boolean isChainValid() {
        synchronized (chain) {
            for (int i = 1; i < chain.size(); i++) {
                LedgerBlock current = chain.get(i);
                LedgerBlock previous = chain.get(i - 1);
                String expected = hash(current.index(), current.timestamp(), current.transactionReference(),
                        current.payload(), current.previousHash());
                if (!expected.equals(current.hash()) || !current.previousHash().equals(previous.hash())) {
                    return false;
                }
            }
            return true;
        }
    }

    private static String hash(long index, Instant timestamp, String ref, String payload, String previousHash) {
        String raw = index + "|" + timestamp + "|" + ref + "|" + payload + "|" + previousHash;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
