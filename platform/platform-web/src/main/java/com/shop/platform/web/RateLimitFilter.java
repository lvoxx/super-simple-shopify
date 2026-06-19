package com.shop.platform.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Per-shop fixed-window rate limiter (scaffold). Phase 0 ships an in-memory limiter so the seam,
 * the 429 response shape, and the per-tenant key exist; Phase 8/9 swap the store for Redis so the
 * window is shared across app replicas. Keyed on the shop header so one noisy tenant cannot
 * exhaust another's budget.
 */
public class RateLimitFilter extends OncePerRequestFilter {

	private static final String SHOP_HEADER = "X-Shop-Id";
	private final int maxRequestsPerWindow;
	private final long windowMillis;
	private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

	public RateLimitFilter(int maxRequestsPerWindow, Duration window) {
		this.maxRequestsPerWindow = maxRequestsPerWindow;
		this.windowMillis = window.toMillis();
	}

	public RateLimitFilter() {
		this(600, Duration.ofMinutes(1));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String key = request.getHeader(SHOP_HEADER);
		if (key != null && !key.isBlank() && !allow(key)) {
			response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
			response.getWriter().write(
					"{\"status\":429,\"code\":\"rate.limited\",\"category\":\"RATE_LIMITED\"}");
			return;
		}
		filterChain.doFilter(request, response);
	}

	private boolean allow(String key) {
		long now = System.currentTimeMillis();
		Window window = windows.compute(key, (k, existing) -> {
			if (existing == null || now - existing.startedAt >= windowMillis) {
				return new Window(now);
			}
			return existing;
		});
		return window.count.incrementAndGet() <= maxRequestsPerWindow;
	}

	private static final class Window {
		private final long startedAt;
		private final AtomicInteger count = new AtomicInteger();

		private Window(long startedAt) {
			this.startedAt = startedAt;
		}
	}
}
