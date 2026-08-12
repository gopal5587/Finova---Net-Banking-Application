package com.finova.account;

/**
 * Lifecycle state of an account.
 *
 * <ul>
 *   <li>{@link #ACTIVE} - normal operation.</li>
 *   <li>{@link #FROZEN} - blocked from debits/credits (e.g. fraud hold); set by admins in Phase 6.</li>
 *   <li>{@link #CLOSED} - permanently deactivated.</li>
 * </ul>
 */
public enum AccountStatus {
    ACTIVE,
    FROZEN,
    CLOSED
}
