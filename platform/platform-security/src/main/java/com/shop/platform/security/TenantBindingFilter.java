package com.shop.platform.security;

import com.shop.platform.core.ShopId;
import com.shop.platform.core.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resolves the tenant (and, for admin calls, the authenticated {@link Principal}) and binds them to
 * {@link TenantContext} and {@link SecurityContext} (JDK 25 {@code ScopedValue}s) for the lifetime of the
 * request. There are two resolution modes, chosen by what the gateway forwards:
 *
 * <ul>
 *   <li><strong>Admin</strong>: the gateway (after Keycloak authenticates) injects {@code X-Shop-Id} plus
 *       the caller's identity in {@code X-Auth-Subject} / {@code X-Auth-Roles}. The shop comes from the
 *       header and a {@code Principal} is bound.</li>
 *   <li><strong>Storefront</strong>: no {@code X-Shop-Id}; the shop is resolved from the request host via
 *       the {@link HostTenantResolver} SPI. Such requests are anonymous — a tenant is bound but no
 *       principal.</li>
 * </ul>
 *
 * <p>A missing/invalid tenant is a hard failure, never a silent default. Some paths are <strong>not</strong>
 * tenant-scoped — notably the control-plane provisioning surface ({@code /api/v1/control/**}), where the
 * shop does not exist yet; those prefixes are passed in and skipped via {@link #shouldNotFilter}. The
 * module is never its own resource server: it trusts the gateway-forwarded headers and does not validate
 * tokens.
 */
public class TenantBindingFilter extends OncePerRequestFilter {

	public static final String SHOP_HEADER = "X-Shop-Id";
	public static final String SUBJECT_HEADER = "X-Auth-Subject";
	public static final String ROLES_HEADER = "X-Auth-Roles";
	public static final String FORWARDED_HOST_HEADER = "X-Forwarded-Host";

	private final List<String> openPathPrefixes;
	private final HostTenantResolver hostResolver;

	public TenantBindingFilter() {
		this(List.of(), null);
	}

	public TenantBindingFilter(List<String> openPathPrefixes) {
		this(openPathPrefixes, null);
	}

	/**
	 * @param openPathPrefixes request-path prefixes that are not tenant-scoped (e.g. the control-plane
	 *                         provisioning surface) and so skip tenant resolution entirely
	 * @param hostResolver     storefront host&rarr;shop resolver, or {@code null} when only header-based
	 *                         (admin) resolution is supported
	 */
	public TenantBindingFilter(List<String> openPathPrefixes, HostTenantResolver hostResolver) {
		this.openPathPrefixes = List.copyOf(openPathPrefixes);
		this.hostResolver = hostResolver;
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
		Principal principal = resolvePrincipal(request, shopId);
		try {
			TenantContext.callWithShop(shopId, () -> {
				if (principal != null) {
					return SecurityContext.callWithPrincipal(principal, () -> {
						filterChain.doFilter(request, response);
						return null;
					});
				}
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
		if (raw != null && !raw.isBlank()) {
			try {
				return ShopId.of(Long.parseLong(raw.trim()));
			}
			catch (NumberFormatException e) {
				throw new TenantResolutionException("Invalid " + SHOP_HEADER + ": " + raw);
			}
		}
		if (hostResolver != null) {
			String host = resolveHost(request);
			if (host != null) {
				return hostResolver.resolveByHost(host)
						.orElseThrow(() -> new TenantResolutionException("No shop for host: " + host));
			}
		}
		throw new TenantResolutionException(
				"Missing " + SHOP_HEADER + " header and no resolvable storefront host");
	}

	/** The storefront host, preferring {@code X-Forwarded-Host} (gateway) over the connection host. */
	private String resolveHost(HttpServletRequest request) {
		String forwarded = request.getHeader(FORWARDED_HOST_HEADER);
		String host = (forwarded != null && !forwarded.isBlank()) ? forwarded : request.getServerName();
		if (host == null || host.isBlank()) {
			return null;
		}
		host = host.trim();
		int comma = host.indexOf(',');          // X-Forwarded-Host may carry a proxy list
		if (comma >= 0) {
			host = host.substring(0, comma).trim();
		}
		int colon = host.indexOf(':');          // strip any port
		if (colon >= 0) {
			host = host.substring(0, colon);
		}
		return host.isBlank() ? null : host.toLowerCase(Locale.ROOT);
	}

	/** Build a {@link Principal} from the forwarded identity headers, or {@code null} for anonymous calls. */
	private Principal resolvePrincipal(HttpServletRequest request, ShopId shopId) {
		String subject = request.getHeader(SUBJECT_HEADER);
		if (subject == null || subject.isBlank()) {
			return null;
		}
		return new Principal(shopId, subject.trim(), parseRoles(request.getHeader(ROLES_HEADER)));
	}

	private static Set<Role> parseRoles(String raw) {
		if (raw == null || raw.isBlank()) {
			return Set.of();
		}
		Set<Role> roles = EnumSet.noneOf(Role.class);
		for (String token : raw.split(",")) {
			String name = token.trim();
			if (name.isEmpty()) {
				continue;
			}
			try {
				roles.add(Role.valueOf(name.toUpperCase(Locale.ROOT)));
			}
			catch (IllegalArgumentException ignored) {
				// An unrecognised role forwarded by the gateway grants nothing — never a hard failure.
			}
		}
		return Set.copyOf(roles);
	}
}
