package com.shop.platform.events;

import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper for the transactional outbox. SQL lives in {@code OutboxMapper.xml} so the
 * statements stay reviewable (the reason this project chose MyBatis over JPA).
 */
@Mapper
public interface OutboxMapper {

	void insert(OutboxRecord record);

	List<OutboxRecord> findPending(@Param("limit") int limit);

	void markProcessed(@Param("eventId") UUID eventId);

	void markFailed(@Param("eventId") UUID eventId, @Param("attempts") int attempts);

	void markDead(@Param("eventId") UUID eventId);
}
