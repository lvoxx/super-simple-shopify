package com.shop.platform.jobs;

/**
 * Handles one delivery of a job/event. Implementations are idempotent and re-establish
 * {@code TenantContext} from {@link JobContext#shopId()} before doing tenant work.
 */
public interface JobHandler {

	void handle(JobContext context);
}
