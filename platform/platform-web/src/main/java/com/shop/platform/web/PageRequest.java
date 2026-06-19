package com.shop.platform.web;

/**
 * Zero-based pagination request, with the page size capped to protect shards from unbounded
 * reads. Bound at the edge, never trusted raw from the client.
 */
public record PageRequest(int page, int size) {

	public static final int MAX_SIZE = 100;
	public static final int DEFAULT_SIZE = 20;

	public PageRequest {
		if (page < 0) {
			page = 0;
		}
		if (size <= 0) {
			size = DEFAULT_SIZE;
		}
		if (size > MAX_SIZE) {
			size = MAX_SIZE;
		}
	}

	public long offset() {
		return (long) page * size;
	}
}
