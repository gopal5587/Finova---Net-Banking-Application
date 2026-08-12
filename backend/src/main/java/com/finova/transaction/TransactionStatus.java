package com.finova.transaction;

/**
 * Outcome of a transaction. Synchronous operations settle immediately as {@link #COMPLETED};
 * {@link #FAILED} is reserved for future asynchronous flows (e.g. external payment settlement).
 */
public enum TransactionStatus {
    COMPLETED,
    FAILED
}
