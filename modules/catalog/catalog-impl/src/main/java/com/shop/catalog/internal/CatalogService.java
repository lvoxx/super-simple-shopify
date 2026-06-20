package com.shop.catalog.internal;

import com.shop.catalog.CatalogFacade;
import com.shop.catalog.CreateProductCommand;
import com.shop.catalog.ProductId;
import com.shop.catalog.ProductPublished;
import com.shop.catalog.ProductStatus;
import com.shop.catalog.ProductView;
import com.shop.catalog.UpdateVariantCommand;
import com.shop.catalog.VariantUpdated;
import com.shop.catalog.VariantView;
import com.shop.platform.core.DomainException;
import com.shop.platform.core.ProblemCategory;
import com.shop.platform.core.ShopId;
import com.shop.platform.core.TenantContext;
import com.shop.platform.core.TimeProvider;
import com.shop.platform.events.DomainEventPublisher;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The catalog module's implementation of {@link CatalogFacade}. Every operation is tenant-scoped: the
 * shop comes from {@link TenantContext#requireShop()} (a hard failure if unbound), never from the caller.
 * Each mutation is one tenant transaction so the state change and its outbox event commit together —
 * publishing a product writes the status change and {@link ProductPublished} atomically; editing a
 * variant writes the change and {@link VariantUpdated} atomically.
 *
 * <p>Catalog owns merchandising data only — it never writes stock. Inventory reacts to these events in a
 * later Phase 2 deliverable; nothing here calls the inventory module synchronously.
 */
@Service
public class CatalogService implements CatalogFacade {

	private final ProductMapper products;
	private final CatalogAssembler assembler;
	private final DomainEventPublisher events;
	private final TimeProvider time;

	public CatalogService(ProductMapper products, CatalogAssembler assembler, DomainEventPublisher events,
			TimeProvider time) {
		this.products = products;
		this.assembler = assembler;
		this.events = events;
		this.time = time;
	}

	@Override
	@Transactional
	public ProductView create(CreateProductCommand command) {
		ShopId shopId = TenantContext.requireShop();
		if (command.variants().isEmpty()) {
			throw new DomainException("catalog.product.no_variants", ProblemCategory.VALIDATION,
					"A product must be created with at least one variant");
		}
		var now = time.now();
		var product = assembler.toNewProductRow(shopId, command, now);
		products.insertProduct(product);
		int position = 1;
		var variantRows = new java.util.ArrayList<VariantRow>();
		for (CreateProductCommand.NewVariant variant : command.variants()) {
			var row = assembler.toNewVariantRow(shopId, product.getId(), variant, position++, now);
			products.insertVariant(row);
			variantRows.add(row);
		}
		return assembler.toProductView(product, variantRows);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ProductView> findProduct(ProductId productId) {
		ShopId shopId = TenantContext.requireShop();
		ProductRow product = products.findProduct(shopId.value(), productId.value());
		if (product == null) {
			return Optional.empty();
		}
		List<VariantRow> variants = products.findVariants(shopId.value(), product.getId());
		return Optional.of(assembler.toProductView(product, variants));
	}

	@Override
	@Transactional
	public ProductView publish(ProductId productId) {
		ShopId shopId = TenantContext.requireShop();
		ProductRow product = requireProduct(shopId, productId);
		if (ProductStatus.ACTIVE.name().equals(product.getStatus())) {
			// Idempotent: already published — no status change, no duplicate event.
			return assembler.toProductView(product, products.findVariants(shopId.value(), product.getId()));
		}
		var now = time.now();
		product.setStatus(ProductStatus.ACTIVE.name());
		product.setPublishedAt(now);
		product.setUpdatedAt(now);
		products.updateStatus(shopId.value(), product.getId(), product.getStatus(), now, now);
		events.publish(new ProductPublished(UUID.randomUUID(), now, shopId,
				ProductId.of(product.getId()), product.getHandle()));
		return assembler.toProductView(product, products.findVariants(shopId.value(), product.getId()));
	}

	@Override
	@Transactional
	public ProductView unpublish(ProductId productId) {
		ShopId shopId = TenantContext.requireShop();
		ProductRow product = requireProduct(shopId, productId);
		if (!ProductStatus.ACTIVE.name().equals(product.getStatus())) {
			// Idempotent: not currently published — nothing to hide.
			return assembler.toProductView(product, products.findVariants(shopId.value(), product.getId()));
		}
		var now = time.now();
		product.setStatus(ProductStatus.DRAFT.name());
		product.setUpdatedAt(now);
		// published_at is cleared so the storefront treats it as never-currently-published.
		product.setPublishedAt(null);
		products.updateStatus(shopId.value(), product.getId(), product.getStatus(), null, now);
		return assembler.toProductView(product, products.findVariants(shopId.value(), product.getId()));
	}

	@Override
	@Transactional
	public VariantView updateVariant(UpdateVariantCommand command) {
		ShopId shopId = TenantContext.requireShop();
		VariantRow row = products.findVariant(shopId.value(), command.variantId().value());
		if (row == null) {
			throw new DomainException("catalog.variant.not_found", ProblemCategory.NOT_FOUND,
					"No variant " + command.variantId().value() + " in shop " + shopId.value());
		}
		var now = time.now();
		row.setSku(command.sku());
		row.setTitle(command.title());
		row.setPriceAmount(command.price().amount());
		row.setPriceCurrency(command.price().currency().getCurrencyCode());
		row.setUpdatedAt(now);
		products.updateVariant(row);
		events.publish(new VariantUpdated(UUID.randomUUID(), now, shopId,
				ProductId.of(row.getProductId()), command.variantId()));
		return assembler.toVariantView(row);
	}

	private ProductRow requireProduct(ShopId shopId, ProductId productId) {
		ProductRow product = products.findProduct(shopId.value(), productId.value());
		if (product == null) {
			throw new DomainException("catalog.product.not_found", ProblemCategory.NOT_FOUND,
					"No product " + productId.value() + " in shop " + shopId.value());
		}
		return product;
	}
}
