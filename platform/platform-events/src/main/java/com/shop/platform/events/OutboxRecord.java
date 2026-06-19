package com.shop.platform.events;

import java.time.Instant;
import java.util.UUID;

/**
 * One row of the {@code platform_outbox} table. A mutable POJO on purpose: it is a MyBatis row
 * mapped by {@link OutboxMapper}, internal to platform-events (never an API type).
 */
public class OutboxRecord {

	private UUID eventId;
	private long shopId;
	private String eventType;
	private String payload;
	private Instant occurredAt;
	private OutboxStatus status = OutboxStatus.PENDING;
	private int attempts;

	public UUID getEventId() {
		return eventId;
	}

	public void setEventId(UUID eventId) {
		this.eventId = eventId;
	}

	public long getShopId() {
		return shopId;
	}

	public void setShopId(long shopId) {
		this.shopId = shopId;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

	public void setOccurredAt(Instant occurredAt) {
		this.occurredAt = occurredAt;
	}

	public OutboxStatus getStatus() {
		return status;
	}

	public void setStatus(OutboxStatus status) {
		this.status = status;
	}

	public int getAttempts() {
		return attempts;
	}

	public void setAttempts(int attempts) {
		this.attempts = attempts;
	}
}
