package com.shop.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.shop.catalog.CreateProductCommand;
import com.shop.catalog.ProductStatus;
import com.shop.catalog.ProductView;
import com.shop.platform.core.Money;
import com.shop.platform.core.ShopId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pure unit test for the catalog row-to-DTO mapping — passes with only catalog-api + platform-core on the
 * classpath (no Spring, no other module impl), satisfying the module-slice isolation rule.
 */
class CatalogAssemblerTest {

	private final CatalogAssembler assembler = new CatalogAssembler();

	@Test
	void newProductRowDefaultsToDraftAndNotPublished() {
		var now = Instant.parse("2026-06-20T10:15:30Z");

		ProductRow row = assembler.toNewProductRow(ShopId.of(7),
				new CreateProductCommand("Tee", "A shirt", "tee", "Acme", "Apparel",
						List.of(new CreateProductCommand.NewVariant("SKU-1", "S", Money.of("19.99", "USD")))),
				now);

		assertThat(row.getShopId()).isEqualTo(7L);
		assertThat(row.getHandle()).isEqualTo("tee");
		assertThat(row.getStatus()).isEqualTo(ProductStatus.DRAFT.name());
		assertThat(row.getPublishedAt()).isNull();
		assertThat(row.getCreatedAt()).isEqualTo(now);
	}

	@Test
	void newVariantRowDecomposesMoneyAndKeepsPosition() {
		var now = Instant.parse("2026-06-20T10:15:30Z");

		VariantRow row = assembler.toNewVariantRow(ShopId.of(7), 42L,
				new CreateProductCommand.NewVariant("SKU-9", "Large", Money.of("19.5", "USD")), 3, now);

		assertThat(row.getProductId()).isEqualTo(42L);
		assertThat(row.getPriceAmount()).isEqualByComparingTo(new BigDecimal("19.50"));
		assertThat(row.getPriceCurrency()).isEqualTo("USD");
		assertThat(row.getPosition()).isEqualTo(3);
	}

	@Test
	void productViewRoundTripsStatusAndVariants() {
		var product = new ProductRow();
		product.setId(100L);
		product.setShopId(7L);
		product.setTitle("Tee");
		product.setHandle("tee");
		product.setStatus(ProductStatus.ACTIVE.name());
		product.setPublishedAt(Instant.parse("2026-06-20T10:15:30Z"));
		product.setCreatedAt(Instant.parse("2026-06-19T09:00:00Z"));

		var variant = new VariantRow();
		variant.setId(200L);
		variant.setShopId(7L);
		variant.setProductId(100L);
		variant.setSku("SKU-1");
		variant.setTitle("S");
		variant.setPriceAmount(new BigDecimal("19.99"));
		variant.setPriceCurrency("USD");
		variant.setPosition(1);

		ProductView view = assembler.toProductView(product, List.of(variant));

		assertThat(view.id().value()).isEqualTo(100L);
		assertThat(view.status()).isEqualTo(ProductStatus.ACTIVE);
		assertThat(view.variants()).hasSize(1);
		assertThat(view.variants().getFirst().price()).isEqualTo(Money.of("19.99", "USD"));
		assertThat(view.variants().getFirst().id().value()).isEqualTo(200L);
	}
}
