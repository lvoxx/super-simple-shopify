package com.shop.platform.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.platform.core.DomainEvent;
import com.shop.platform.persistence.ControlPlaneConfig;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publishes <strong>control-plane</strong> domain events (e.g. {@code ShopCreated}) to the control
 * outbox in the caller's control-plane transaction, then raises them as in-process Spring events.
 * The {@code job-engine} drains the control outbox asynchronously and idempotently (keyed on
 * {@code eventId}).
 *
 * <p>This is a distinct bean from the {@code @Primary} tenant {@link OutboxEventPublisher}: callers
 * inject it explicitly because a control-plane event has no tenant shard to be written to. It runs
 * under the {@code controlTransactionManager}, so the event row commits atomically with the shop /
 * shard-assignment writes — no distributed transaction.
 */
@Component
public class ControlPlaneEventPublisher {

	private final ControlOutboxMapper outbox;
	private final ApplicationEventPublisher springEvents;
	private final ObjectMapper objectMapper;

	public ControlPlaneEventPublisher(ControlOutboxMapper outbox, ApplicationEventPublisher springEvents,
			ObjectMapper objectMapper) {
		this.outbox = outbox;
		this.springEvents = springEvents;
		this.objectMapper = objectMapper;
	}

	@Transactional(ControlPlaneConfig.TRANSACTION_MANAGER)
	public void publish(DomainEvent event) {
		var row = new OutboxRecord();
		row.setEventId(event.eventId());
		row.setShopId(event.shopId().value());
		row.setEventType(event.getClass().getName());
		row.setPayload(serialize(event));
		row.setOccurredAt(event.occurredAt());
		row.setStatus(OutboxStatus.PENDING);
		row.setAttempts(0);
		outbox.insert(row);
		springEvents.publishEvent(event);
	}

	private String serialize(DomainEvent event) {
		try {
			return objectMapper.writeValueAsString(event);
		}
		catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Event not serialisable: " + event.getClass().getName(), e);
		}
	}
}
