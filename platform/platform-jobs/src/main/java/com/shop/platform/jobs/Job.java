package com.shop.platform.jobs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link JobHandler} as the handler for a named job/event type. Handlers MUST be
 * idempotent — re-delivery of the same {@code eventId} is a no-op.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Job {

	/** The event/job type this handler consumes (e.g. fully-qualified event class name). */
	String value();

	int maxAttempts() default 5;
}
