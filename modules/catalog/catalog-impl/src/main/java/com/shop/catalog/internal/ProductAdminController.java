package com.shop.catalog.internal;

import com.shop.catalog.CatalogFacade;
import com.shop.catalog.CreateProductCommand;
import com.shop.catalog.ProductId;
import com.shop.catalog.ProductView;
import com.shop.catalog.UpdateVariantCommand;
import com.shop.catalog.VariantId;
import com.shop.catalog.VariantView;
import com.shop.platform.core.DomainException;
import com.shop.platform.core.Money;
import com.shop.platform.core.ProblemCategory;
import com.shop.platform.web.ApiVersion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin surface for managing a shop's catalog. These endpoints are <strong>tenant-scoped</strong>: they
 * live under {@link ApiVersion#V1_PREFIX}, so the tenant-binding filter resolves {@code X-Shop-Id} and
 * binds {@code TenantContext} before the service runs. In the deployed topology the gateway + Keycloak
 * authenticate the caller; the module is not its own resource server. Inbound DTOs are the only place
 * edge validation is declared; the controller never reaches into persistence.
 */
@RestController
@RequestMapping(ApiVersion.V1_PREFIX + "/products")
public class ProductAdminController {

	private final CatalogFacade catalog;

	public ProductAdminController(CatalogFacade catalog) {
		this.catalog = catalog;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ProductView create(@Valid @RequestBody CreateProductRequest request) {
		var variants = request.variants().stream()
				.map(v -> new CreateProductCommand.NewVariant(v.sku(), v.title(), v.toMoney()))
				.toList();
		return catalog.create(new CreateProductCommand(request.title(), request.description(),
				request.handle(), request.vendor(), request.productType(), variants));
	}

	@GetMapping("/{productId}")
	public ProductView get(@PathVariable long productId) {
		return catalog.findProduct(ProductId.of(productId))
				.orElseThrow(() -> new DomainException("catalog.product.not_found", ProblemCategory.NOT_FOUND,
						"No product " + productId));
	}

	@PostMapping("/{productId}/publish")
	public ProductView publish(@PathVariable long productId) {
		return catalog.publish(ProductId.of(productId));
	}

	@PostMapping("/{productId}/unpublish")
	public ProductView unpublish(@PathVariable long productId) {
		return catalog.unpublish(ProductId.of(productId));
	}

	@PutMapping("/variants/{variantId}")
	public VariantView updateVariant(@PathVariable long variantId,
			@Valid @RequestBody UpdateVariantRequest request) {
		return catalog.updateVariant(new UpdateVariantCommand(
				VariantId.of(variantId), request.sku(), request.title(), request.toMoney()));
	}

	/** Inbound request DTO for creating a product with its variants. */
	public record CreateProductRequest(
			@NotBlank String title,
			String description,
			@NotBlank String handle,
			String vendor,
			String productType,
			@NotEmpty @Valid List<VariantRequest> variants) {
	}

	/** A variant within a create request. */
	public record VariantRequest(
			String sku,
			@NotBlank String title,
			@NotNull @DecimalMin("0.0") BigDecimal priceAmount,
			@NotBlank @Size(min = 3, max = 3) String priceCurrency) {

		Money toMoney() {
			return new Money(priceAmount, java.util.Currency.getInstance(priceCurrency));
		}
	}

	/** Inbound request DTO for editing a variant. */
	public record UpdateVariantRequest(
			String sku,
			@NotBlank String title,
			@NotNull @DecimalMin("0.0") BigDecimal priceAmount,
			@NotBlank @Size(min = 3, max = 3) String priceCurrency) {

		Money toMoney() {
			return new Money(priceAmount, java.util.Currency.getInstance(priceCurrency));
		}
	}
}
