package com.shop.platform.events;

import com.shop.platform.core.DomainEvent;

/**
 * SPI for shipping events beyond the monolith (a future Kafka bridge, Phase 12). No-op in
 * Phase 0 but the seam exists so externalization is a config change, not a rewrite.
 */
public interface EventExternalizer {

	void externalize(DomainEvent event);
}
