package com.shop.store.internal;

import com.shop.platform.core.DomainException;
import com.shop.platform.core.ProblemCategory;
import com.shop.platform.core.ShopId;
import com.shop.platform.web.ApiVersion;
import com.shop.store.CreateShopCommand;
import com.shop.store.ShopPlan;
import com.shop.store.ShopView;
import com.shop.store.StoreFacade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Control-plane surface for provisioning shops. These endpoints live under
 * {@link ApiVersion#CONTROL_PREFIX} and are intentionally <strong>not</strong> tenant-scoped — a shop
 * does not exist yet when it is being created, so the tenant-binding filter skips this path. In the
 * deployed topology this surface sits behind the gateway + Keycloak and is restricted to platform
 * operators; the modules never act as their own resource servers.
 */
@RestController
@RequestMapping(ApiVersion.CONTROL_PREFIX + "/shops")
public class StoreControlController {

	private final StoreFacade store;

	public StoreControlController(StoreFacade store) {
		this.store = store;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ShopView create(@Valid @RequestBody CreateShopRequest request) {
		return store.createShop(new CreateShopCommand(
				request.name(), request.plan(), request.locale(), request.primaryDomain()));
	}

	@GetMapping("/{shopId}")
	public ShopView get(@PathVariable long shopId) {
		return store.findShop(ShopId.of(shopId))
				.orElseThrow(() -> new DomainException("store.shop.not_found", ProblemCategory.NOT_FOUND,
						"No shop with id " + shopId));
	}

	/** Inbound request DTO — the only place edge validation is declared. */
	public record CreateShopRequest(
			@NotBlank String name,
			ShopPlan plan,
			@NotBlank String locale,
			@NotBlank String primaryDomain) {
	}
}
