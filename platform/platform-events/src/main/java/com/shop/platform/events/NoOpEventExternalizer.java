package com.shop.platform.events;

import com.shop.platform.core.DomainEvent;
import org.springframework.stereotype.Component;

/** Default externalizer: does nothing. Replaced when a module triggers extraction (Phase 12). */
@Component
public class NoOpEventExternalizer implements EventExternalizer {

	@Override
	public void externalize(DomainEvent event) {
		// Intentionally a no-op: all consumers are in-process in the monolith.
	}
}
