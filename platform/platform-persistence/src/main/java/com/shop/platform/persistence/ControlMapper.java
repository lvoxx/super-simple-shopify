package com.shop.platform.persistence;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a MyBatis mapper as a <strong>control-plane</strong> mapper: it reads/writes the global,
 * non-sharded control datastore (shop registry, shop&rarr;shard map, control outbox) rather than a
 * tenant shard. Such mappers are bound to the control {@code SqlSessionFactory} and execute against
 * {@link ControlPlaneConfig#controlDataSource}, NOT the {@link TenantRoutingDataSource}.
 *
 * <p>Deliberately <em>not</em> meta-annotated with {@code @Mapper}: the tenant mapper scanner only
 * registers {@code @Mapper}-annotated interfaces, so a {@code @ControlMapper} interface is invisible
 * to it and is registered exclusively against the control factory. A mapper carries one annotation
 * or the other — never both — which keeps the two datasources strictly separated.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ControlMapper {
}
