package com.shop.platform.events;

import com.shop.platform.core.DomainEvent;

/**
 * The one legal way to raise a domain event. Implementations persist the event to the outbox in
 * the SAME transaction as the state change, so a committed change never loses its event and a
 * rolled-back change never emits one.
 */
public interface DomainEventPublisher {

	void publish(DomainEvent event);
}
