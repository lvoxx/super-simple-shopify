package com.shop.platform.security;

import com.shop.platform.core.ShopId;
import com.shop.platform.core.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Scaffold filter that resolves the tenant and binds it to {@link TenantContext} (a JDK 25
 * {@code ScopedValue}) for the lifetime of the request. Phase 0 resolves the shop from the
 * {@code X-Shop-Id} header; Phase 1 replaces this with host/domain + authenticated-session
 * resolution. A missing/invalid tenant is a hard failure, never a silent default.
 */
public class TenantBindingFilter extends OncePerRequestFilter {

	public static final String SHOP_HEADER = "X-Shop-Id";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		ShopId shopId = resolveShop(request);
		try {
			TenantContext.callWithShop(shopId, () -> {
				filterChain.doFilter(request, response);
				return null;
			});
		}
		catch (ServletException | IOException e) {
			throw e;
		}
		catch (Exception e) {
			throw new ServletException(e);
		}
	}

	private ShopId resolveShop(HttpServletRequest request) {
		String raw = request.getHeader(SHOP_HEADER);
		if (raw == null || raw.isBlank()) {
			throw new TenantResolutionException("Missing " + SHOP_HEADER + " header");
		}
		try {
			return ShopId.of(Long.parseLong(raw.trim()));
		}
		catch (NumberFormatException e) {
			throw new TenantResolutionException("Invalid " + SHOP_HEADER + ": " + raw);
		}
	}
}
