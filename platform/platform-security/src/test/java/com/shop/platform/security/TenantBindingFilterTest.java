package com.shop.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shop.platform.core.ShopId;
import com.shop.platform.core.TenantContext;
import jakarta.servlet.FilterChain;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Unit test for the tenant/principal binding filter. A capturing {@link FilterChain} records what is
 * bound to {@link TenantContext} / {@link SecurityContext} <em>inside</em> the scope, since the bindings
 * only live for the duration of the chain. No Spring context, no servlet container.
 */
class TenantBindingFilterTest {

	/** Records the context state observed while the request is being handled. */
	private static final class Captor implements FilterChain {
		boolean invoked;
		ShopId shop;
		Principal principal;

		@Override
		public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
			invoked = true;
			shop = TenantContext.currentShop().orElse(null);
			principal = SecurityContext.currentPrincipal().orElse(null);
		}
	}

	@Test
	void adminRequestBindsShopFromHeaderAndPrincipalFromIdentityHeaders() throws Exception {
		var filter = new TenantBindingFilter(List.of());
		var request = new MockHttpServletRequest("GET", "/api/v1/staff");
		request.addHeader(TenantBindingFilter.SHOP_HEADER, "7");
		request.addHeader(TenantBindingFilter.SUBJECT_HEADER, "kc-sub-1");
		request.addHeader(TenantBindingFilter.ROLES_HEADER, "OWNER, admin, bogus");
		var captor = new Captor();

		filter.doFilter(request, new MockHttpServletResponse(), captor);

		assertThat(captor.invoked).isTrue();
		assertThat(captor.shop).isEqualTo(ShopId.of(7));
		assertThat(captor.principal).isNotNull();
		assertThat(captor.principal.subject()).isEqualTo("kc-sub-1");
		// Case-insensitive parse; unknown roles ("bogus") grant nothing.
		assertThat(captor.principal.roles()).containsExactlyInAnyOrder(Role.OWNER, Role.ADMIN);
	}

	@Test
	void storefrontRequestResolvesShopByHostAndIsAnonymous() throws Exception {
		HostTenantResolver resolver = host ->
				"acme.example".equals(host) ? Optional.of(ShopId.of(42)) : Optional.empty();
		var filter = new TenantBindingFilter(List.of(), resolver);
		var request = new MockHttpServletRequest("GET", "/api/v1/storefront/products");
		request.addHeader(TenantBindingFilter.FORWARDED_HOST_HEADER, "Acme.Example:443");
		var captor = new Captor();

		filter.doFilter(request, new MockHttpServletResponse(), captor);

		assertThat(captor.shop).isEqualTo(ShopId.of(42));
		assertThat(captor.principal).isNull();
	}

	@Test
	void missingTenantIsHardFailure() {
		var filter = new TenantBindingFilter(List.of());
		var request = new MockHttpServletRequest("GET", "/api/v1/storefront/products");
		request.setServerName("");

		assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), new Captor()))
				.isInstanceOf(TenantResolutionException.class);
	}

	@Test
	void unresolvableHostIsHardFailure() {
		HostTenantResolver resolver = host -> Optional.empty();
		var filter = new TenantBindingFilter(List.of(), resolver);
		var request = new MockHttpServletRequest("GET", "/api/v1/storefront/products");
		request.addHeader(TenantBindingFilter.FORWARDED_HOST_HEADER, "unknown.example");

		assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), new Captor()))
				.isInstanceOf(TenantResolutionException.class);
	}

	@Test
	void controlPlanePrefixSkipsTenantBinding() throws Exception {
		var filter = new TenantBindingFilter(List.of("/api/v1/control"));
		var request = new MockHttpServletRequest("POST", "/api/v1/control/shops");
		var captor = new Captor();

		filter.doFilter(request, new MockHttpServletResponse(), captor);

		assertThat(captor.invoked).isTrue();
		assertThat(captor.shop).isNull();
		assertThat(captor.principal).isNull();
	}
}
