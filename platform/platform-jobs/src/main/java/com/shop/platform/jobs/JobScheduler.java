package com.shop.platform.jobs;

/**
 * Schedules deferred/recurring work. Phase 0 ships the contract; the {@code job-engine} provides
 * the outbox-draining runtime that satisfies it.
 */
public interface JobScheduler {

	void enqueue(JobContext context);
}
