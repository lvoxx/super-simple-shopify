package com.shop.hello;

import com.shop.platform.core.ProblemCategory;
import com.shop.platform.core.DomainException;
import com.shop.platform.core.ShopId;
import com.shop.platform.core.TenantContext;
import com.shop.platform.core.TimeProvider;
import com.shop.platform.events.DomainEventPublisher;
import com.shop.platform.web.ApiVersion;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phase 0 spine demonstrator. Reads the current tenant's greeting from its shard and publishes a
 * {@link HelloRequestedEvent} through the outbox in the same transaction as the read's context.
 */
@RestController
@RequestMapping(ApiVersion.V1_PREFIX + "/hello")
public class HelloController {

	private final HelloMapper helloMapper;
	private final DomainEventPublisher events;
	private final TimeProvider time;

	public HelloController(HelloMapper helloMapper, DomainEventPublisher events, TimeProvider time) {
		this.helloMapper = helloMapper;
		this.events = events;
		this.time = time;
	}

	@GetMapping
	public HelloResponse hello() {
		ShopId shop = TenantContext.requireShop();
		String message = helloMapper.findMessage(shop.value());
		if (message == null) {
			throw new DomainException("hello.not_found", ProblemCategory.NOT_FOUND,
					"No greeting configured for shop " + shop.value());
		}
		events.publish(new HelloRequestedEvent(UUID.randomUUID(), time.now(), shop, message));
		return new HelloResponse(shop.value(), message);
	}

	/** Response DTO (record, per the API conventions). */
	public record HelloResponse(long shopId, String message) {
	}
}
