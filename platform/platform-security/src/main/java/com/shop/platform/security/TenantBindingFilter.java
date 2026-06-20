package com.shop.platform.security;

import com.shop.platform.core.ShopId;
import com.shop.platform.core.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resolves the tenant and binds it to {@link TenantContext} (a JDK 25 {@code ScopedValue}) for the
 * lifetime of the request. Phase 0 resolves the shop from the {@code X-Shop-Id} header (injected by the
 * gateway after authentication); Phase 1+ adds host/domain resolution. A missing/invalid tenant is a
 * hard failure, never a silent default.
 *
 * <p>Some paths are <strong>not</strong> tenant-scoped — notably the control-plane provisioning surface
 * ({@code /api/v1/control/**}), where the shop does not exist yet. Those prefixes are passed in and
 * skipped via {@link #shouldNotFilter}.
 */
public class TenantBindingFilter extends OncePerRequestFilter {

	public static final String SHOP_HEADER = "X-Shop-Id";

	private final List<String> openPathPrefixes;

	public TenantBindingFilter() {
		this(List.of());
	}

	/**
	 * @param openPathPrefixes request-path prefixes that are not tenant-scoped (e.g. the control-plane
	 *                         provisioning surface) and so skip tenant resolution entirely
	 */
	public TenantBindingFilter(List<String> openPathPrefixes) {
		this.openPathPrefixes = List.copyOf(openPathPrefixes);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		return openPathPrefixes.stream().anyMatch(path::startsWith);
	}

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
