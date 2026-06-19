package com.shop.platform.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.platform.core.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default publisher: writes the event to the outbox in the caller's transaction, then raises it
 * as an in-process Spring event for decoupled subscribers. The job-engine drains the outbox row
 * asynchronously and idempotently (keyed on {@code eventId}).
 */
@Component
public class OutboxEventPublisher implements DomainEventPublisher {

	private final OutboxMapper outbox;
	private final ApplicationEventPublisher springEvents;
	private final ObjectMapper objectMapper;

	public OutboxEventPublisher(OutboxMapper outbox, ApplicationEventPublisher springEvents,
			ObjectMapper objectMapper) {
		this.outbox = outbox;
		this.springEvents = springEvents;
		this.objectMapper = objectMapper;
	}

	@Override
	@Transactional
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
