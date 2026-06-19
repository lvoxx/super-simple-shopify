package com.shop.jobs.engine;

import com.shop.platform.core.ShopId;
import com.shop.platform.core.TenantContext;
import com.shop.platform.events.OutboxMapper;
import com.shop.platform.events.OutboxRecord;
import com.shop.platform.jobs.JobContext;
import com.shop.platform.jobs.JobHandler;
import com.shop.platform.jobs.RetryPolicy;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls each shard's transactional outbox and dispatches every pending event to its
 * {@link JobHandler} inside the originating tenant's scope. Delivery is at-least-once and
 * idempotent: handlers de-dupe on {@code eventId}, and a row is only marked {@code PROCESSED}
 * after its handler returns. Exhausted retries are dead-lettered, never lost.
 *
 * <p>The outbox scan is shard infrastructure, not a tenant request, so the drainer binds a
 * representative tenant per shard to drive the shard-routing datasource (a missing tenant is a
 * hard failure, so there is no implicit "no shard" path). Phase 0 runs {@code shard-count=1};
 * Phase 8 gives each shard its own transaction boundary.
 */
@Component
public class OutboxDrainer {

	private static final Logger log = LoggerFactory.getLogger(OutboxDrainer.class);
	private static final int BATCH_SIZE = 100;

	private final OutboxMapper outbox;
	private final JobHandlerRegistry registry;
	private final RetryPolicy retryPolicy = RetryPolicy.defaults();
	private final int shardCount;

	public OutboxDrainer(OutboxMapper outbox, JobHandlerRegistry registry,
			@Value("${platform.persistence.shard-count:1}") int shardCount) {
		this.outbox = outbox;
		this.registry = registry;
		this.shardCount = shardCount;
	}

	@Scheduled(fixedDelayString = "${platform.jobs.poll-interval-ms:1000}")
	public void drain() {
		for (int shard = 0; shard < shardCount; shard++) {
			// A shopId congruent to the shard index routes the scan to that shard. findPending is
			// not filtered by shop, so it reads the whole shard's outbox.
			ShopId sweepKey = ShopId.of(shard + shardCount);
			TenantContext.runWithShop(sweepKey, this::drainCurrentShard);
		}
	}

	private void drainCurrentShard() {
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
			log.debug("Delivered outbox event {} ({}) on attempt {}", row.getEventId(), row.getEventType(), attempt);
		}
		catch (RuntimeException ex) {
			if (retryPolicy.exhausted(attempt)) {
				outbox.markDead(row.getEventId());
				log.error("Dead-lettering outbox event {} ({}) after {} attempts",
						row.getEventId(), row.getEventType(), attempt, ex);
			}
			else {
				outbox.markFailed(row.getEventId(), attempt);
				log.warn("Retrying outbox event {} ({}) after attempt {}: {}",
						row.getEventId(), row.getEventType(), attempt, ex.toString());
			}
		}
	}
}
