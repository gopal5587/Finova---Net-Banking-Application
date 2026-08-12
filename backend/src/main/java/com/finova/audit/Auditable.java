package com.finova.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method whose invocation should be recorded in the audit log. Applied via
 * {@link AuditAspect}, which captures the actor, outcome, and (on failure) the error message.
 *
 * <p>Declaring auditing as an annotation keeps the cross-cutting concern out of business code:
 * the service method stays focused on its logic and simply advertises "audit me".
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /** Business action name, e.g. {@code "TRANSFER"} or {@code "ACCOUNT_CREATE"}. */
    String action();

    /** Optional entity/category the action targets, e.g. {@code "Account"}. */
    String targetType() default "";
}
