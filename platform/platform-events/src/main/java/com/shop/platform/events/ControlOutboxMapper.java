package com.shop.platform.events;

import com.shop.platform.persistence.ControlMapper;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper for the <strong>control-plane</strong> outbox ({@code control_outbox}). Mirrors
 * {@link OutboxMapper} but is a {@link ControlMapper}, so it executes against the global control
 * datastore rather than a tenant shard. Control-plane events (e.g. {@code ShopCreated}) are written
 * here in the same transaction as the control-plane state change that raised them — there is no
 * tenant shard to write to yet, and crossing to a shard would be a forbidden distributed transaction.
 */
@ControlMapper
public interface ControlOutboxMapper {

	void insert(OutboxRecord record);

	List<OutboxRecord> findPending(@Param("limit") int limit);

	void markProcessed(@Param("eventId") UUID eventId);

	void markFailed(@Param("eventId") UUID eventId, @Param("attempts") int attempts);

	void markDead(@Param("eventId") UUID eventId);
}
