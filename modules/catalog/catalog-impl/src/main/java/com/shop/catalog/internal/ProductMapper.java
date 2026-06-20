package com.shop.catalog.internal;

import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Tenant mapper for products and their variants. A plain {@code @Mapper}, so it runs against the
 * shard-routed tenant datasource (resolved from {@code TenantContext}). Every statement still filters by
 * {@code shop_id}: a shard physically holds many shops, so routing is necessary but not sufficient for
 * isolation. Variants are loaded with a separate query rather than a join, so a product with N variants
 * is one product row plus one variant list (no cartesian fan-out).
 */
@Mapper
public interface ProductMapper {

	/** Insert a product; the shard allocates {@code id}, written back onto the row. */
	void insertProduct(ProductRow row);

	/** Insert a variant; the shard allocates {@code id}, written back onto the row. */
	void insertVariant(VariantRow row);

	ProductRow findProduct(@Param("shopId") long shopId, @Param("id") long id);

	List<VariantRow> findVariants(@Param("shopId") long shopId, @Param("productId") long productId);

	VariantRow findVariant(@Param("shopId") long shopId, @Param("id") long id);

	void updateStatus(@Param("shopId") long shopId, @Param("id") long id, @Param("status") String status,
			@Param("publishedAt") Instant publishedAt, @Param("updatedAt") Instant updatedAt);

	void updateVariant(VariantRow row);
}
