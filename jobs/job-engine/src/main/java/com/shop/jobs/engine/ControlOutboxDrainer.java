package com.shop.jobs.engine;

import com.shop.platform.core.ShopId;
import com.shop.platform.core.TenantContext;
import com.shop.platform.events.ControlOutboxMapper;
import com.shop.platform.events.OutboxRecord;
import com.shop.platform.jobs.JobContext;
import com.shop.platform.jobs.JobHandler;
import com.shop.platform.jobs.RetryPolicy;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Drains the <strong>control-plane</strong> outbox ({@code control_outbox}) and dispatches each
 * pending event to its {@link JobHandler}. The control plane is a single, non-sharded datastore, so —
 * unlike {@link OutboxDrainer} — there is no per-shard sweep and the scan needs no bound tenant; the
 * control mapper reads the global control datasource directly. Handler execution still binds the
 * event's tenant ({@link JobContext#shopId()}) so a handler can do tenant work on the new shop's shard.
 * Delivery is at-least-once and idempotent (keyed on {@code eventId}); exhausted retries are dead-lettered.
 */
@Component
public class ControlOutboxDrainer {

	private static final Logger log = LoggerFactory.getLogger(ControlOutboxDrainer.class);
	private static final int BATCH_SIZE = 100;

	private final ControlOutboxMapper outbox;
	private final JobHandlerRegistry registry;
	private final RetryPolicy retryPolicy = RetryPolicy.defaults();

	public ControlOutboxDrainer(ControlOutboxMapper outbox, JobHandlerRegistry registry) {
		this.outbox = outbox;
		this.registry = registry;
	}

	@Scheduled(fixedDelayString = "${platform.jobs.poll-interval-ms:1000}")
	public void drain() {
		for (OutboxRecord row : outbox.findPending(BATCH_SIZE)) {
			dispatch(row);
		}
	}

	private void dispatch(OutboxRecord row) {
		Optional<JobHandler> handler = registry.handlerFor(row.getEventType());
		if (handler.isEmpty()) {
			outbox.markProcessed(row.getEventId());
			return;
		}
		int attempt = row.getAttempts() + 1;
		var context = new JobContext(row.getEventId(), ShopId.of(row.getShopId()),
				row.getEventType(), row.getPayload(), attempt);
		try {
			TenantContext.runWithShop(context.shopId(), () -> handler.get().handle(context));
			outbox.markProcessed(row.getEventId());
			log.debug("Delivered control event {} ({}) on attempt {}", row.getEventId(), row.getEventType(), attempt);
		}
		catch (RuntimeException ex) {
			if (retryPolicy.exhausted(attempt)) {
				outbox.markDead(row.getEventId());
				log.error("Dead-lettering control event {} ({}) after {} attempts",
						row.getEventId(), row.getEventType(), attempt, ex);
			}
			else {
				outbox.markFailed(row.getEventId(), attempt);
				log.warn("Retrying control event {} ({}) after attempt {}: {}",
						row.getEventId(), row.getEventType(), attempt, ex.toString());
			}
		}
	}
}
