package com.shop.hello;

import com.shop.platform.jobs.Job;
import com.shop.platform.jobs.JobContext;
import com.shop.platform.jobs.JobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Async tail of the slice: the job-engine drains the outbox row and invokes this idempotent
 * handler, which simply logs. Keyed on the event class name so the drainer can route to it.
 */
@Component
@Job(value = "com.shop.hello.HelloRequestedEvent", maxAttempts = 3)
public class HelloLoggedJobHandler implements JobHandler {

	private static final Logger log = LoggerFactory.getLogger(HelloLoggedJobHandler.class);

	@Override
	public void handle(JobContext context) {
		log.info("hello tenant {} — delivered via outbox (event {}): {}",
				context.shopId(), context.eventId(), context.payload());
	}
}
