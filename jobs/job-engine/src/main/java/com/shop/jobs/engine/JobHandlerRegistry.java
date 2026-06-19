package com.shop.jobs.engine;

import com.shop.platform.jobs.Job;
import com.shop.platform.jobs.JobHandler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

/**
 * Indexes every {@link JobHandler} bean by the event type its {@link Job} annotation declares,
 * so the drainer can route an outbox row to its handler in O(1). Unmapped event types are simply
 * acknowledged (no consumer in this deployment).
 */
@Component
public class JobHandlerRegistry {

	private final Map<String, JobHandler> handlersByType = new HashMap<>();

	public JobHandlerRegistry(List<JobHandler> handlers) {
		for (JobHandler handler : handlers) {
			Job job = AnnotatedElementUtils.findMergedAnnotation(AopUtils.getTargetClass(handler), Job.class);
			if (job != null) {
				handlersByType.put(job.value(), handler);
			}
		}
	}

	public Optional<JobHandler> handlerFor(String eventType) {
		return Optional.ofNullable(handlersByType.get(eventType));
	}
}
