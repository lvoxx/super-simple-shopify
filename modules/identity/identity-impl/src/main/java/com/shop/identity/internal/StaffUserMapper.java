package com.shop.identity.internal;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Tenant mapper for staff users. A plain {@code @Mapper}, so it runs against the shard-routed tenant
 * datasource (resolved from {@code TenantContext}) — unlike the store module's control-plane mappers.
 * Every statement still filters by {@code shop_id}: a shard physically holds many shops, so routing to
 * the right datasource is necessary but not sufficient for isolation.
 */
@Mapper
public interface StaffUserMapper {

	void insert(StaffUserRow row);

	void insertRole(@Param("shopId") long shopId, @Param("subject") String subject, @Param("role") String role);

	/** Load a staff user with its roles collected, or {@code null} if no such subject in this shop. */
	StaffUserRow findBySubject(@Param("shopId") long shopId, @Param("subject") String subject);
}
