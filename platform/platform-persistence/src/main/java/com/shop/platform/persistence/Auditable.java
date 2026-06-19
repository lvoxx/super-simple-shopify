package com.shop.platform.persistence;

import java.time.Instant;

/**
 * Audit columns every persistent row carries. Concrete rows in domain {@code *-impl} modules
 * embed these; {@code shop_id} is mandatory on every tenant table (enforced by migrations and
 * the pre-implementation gate), so it is modelled here as a first-class field.
 */
public interface Auditable {

	long shopId();

	Instant createdAt();

	Instant updatedAt();
}
