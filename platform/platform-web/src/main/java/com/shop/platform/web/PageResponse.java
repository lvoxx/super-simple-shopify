package com.shop.platform.web;

import java.util.List;

/** A page of results plus the metadata a client needs to page through the rest. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements) {

	public PageResponse {
		content = List.copyOf(content);
	}

	public int totalPages() {
		return size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
	}

	public static <T> PageResponse<T> of(List<T> content, PageRequest request, long totalElements) {
		return new PageResponse<>(content, request.page(), request.size(), totalElements);
	}
}
