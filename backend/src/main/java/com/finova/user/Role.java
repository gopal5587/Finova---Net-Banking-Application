package com.finova.user;

/**
 * Application roles. Kept intentionally small; fine-grained permissions can be layered
 * on later if needed. {@link #ADMIN} unlocks fraud/oversight endpoints (Phase 6).
 */
public enum Role {
    USER,
    ADMIN
}
