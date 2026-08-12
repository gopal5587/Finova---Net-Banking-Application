package com.finova.audit;

import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Stamps each Envers revision with the acting username.
 *
 * <p>Falls back to {@code "system"} for background jobs / scheduled tasks that run without an
 * authenticated principal, so a revision is never left unattributed.
 */
public class FinovaRevisionListener implements RevisionListener {

    private static final String SYSTEM_ACTOR = "system";

    @Override
    public void newRevision(Object revisionEntity) {
        FinovaRevisionEntity revision = (FinovaRevisionEntity) revisionEntity;
        revision.setUsername(currentUsername());
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return SYSTEM_ACTOR;
        }
        return auth.getName();
    }
}
