package com.shop.hello;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Reads the tenant's greeting. The query filters by {@code shop_id} and runs through the
 * shard-routing datasource, so it only ever touches the current tenant's shard.
 */
@Mapper
public interface HelloMapper {

	String findMessage(@Param("shopId") long shopId);
}
