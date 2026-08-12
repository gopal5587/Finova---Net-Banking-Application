package com.finova.transaction;

public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER,
    /** Credited by the interest accrual scheduler for eligible savings accounts. */
    INTEREST
}
