package com.shop.catalog.internal;

import com.shop.catalog.CreateProductCommand;
import com.shop.catalog.ProductId;
import com.shop.catalog.ProductStatus;
import com.shop.catalog.ProductView;
import com.shop.catalog.VariantId;
import com.shop.catalog.VariantView;
import com.shop.platform.core.Money;
import com.shop.platform.core.ShopId;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Centralizes the catalog module's row-to-DTO mapping. As in the identity and store slices, ModelMapper
 * is the project default but the edges here cross record, enum, and value-object boundaries (the API uses
 * records with {@code ProductStatus} and {@code Money}, while rows store enum names and a decomposed
 * amount/currency), where explicit construction is clearer. Mapping stays here, never inlined into the
 * controller or persistence.
 */
@Component
public class CatalogAssembler {

	/** Build a new product row from a create command, defaulting status to {@link ProductStatus#DRAFT}. */
	public ProductRow toNewProductRow(ShopId shopId, CreateProductCommand command, Instant now) {
		var row = new ProductRow();
		row.setShopId(shopId.value());
		row.setTitle(command.title());
		row.setDescription(command.description());
		row.setHandle(command.handle());
		row.setStatus(ProductStatus.DRAFT.name());
		row.setVendor(command.vendor());
		row.setProductType(command.productType());
		row.setPublishedAt(null);
		row.setCreatedAt(now);
		row.setUpdatedAt(now);
		return row;
	}

	/** Build a new variant row under {@code productId} at {@code position}. */
	public VariantRow toNewVariantRow(ShopId shopId, long productId, CreateProductCommand.NewVariant variant,
			int position, Instant now) {
		var row = new VariantRow();
		row.setShopId(shopId.value());
		row.setProductId(productId);
		row.setSku(variant.sku());
		row.setTitle(variant.title());
		row.setPriceAmount(variant.price().amount());
		row.setPriceCurrency(variant.price().currency().getCurrencyCode());
		row.setPosition(position);
		row.setCreatedAt(now);
		row.setUpdatedAt(now);
		return row;
	}

	public VariantView toVariantView(VariantRow row) {
		return new VariantView(
				VariantId.of(row.getId()),
				ProductId.of(row.getProductId()),
				row.getSku(),
				row.getTitle(),
				new Money(row.getPriceAmount(), Currency.getInstance(row.getPriceCurrency())),
				row.getPosition());
	}

	public ProductView toProductView(ProductRow row, List<VariantRow> variants) {
		return new ProductView(
				ProductId.of(row.getId()),
				ShopId.of(row.getShopId()),
				row.getTitle(),
				row.getDescription(),
				row.getHandle(),
				ProductStatus.valueOf(row.getStatus()),
				row.getVendor(),
				row.getProductType(),
				row.getPublishedAt(),
				row.getCreatedAt(),
				variants.stream().map(this::toVariantView).toList());
	}
}
