package com.shop;

import com.shop.platform.security.HostTenantResolver;
import com.shop.platform.security.TenantBindingFilter;
import com.shop.platform.web.ApiVersion;
import com.shop.platform.web.RateLimitFilter;
import com.shop.store.ShopView;
import com.shop.store.StoreFacade;
import java.util.List;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the cross-cutting servlet filters in order: rate-limit first (cheap reject before any
 * work), then tenant binding (establishes the {@code ScopedValue} every downstream layer relies on).
 * The tenant filter skips the control-plane prefix ({@link ApiVersion#CONTROL_PREFIX}), whose endpoints
 * provision shops before any tenant exists.
 *
 * <p>The app is the composition root, so it supplies the storefront {@link HostTenantResolver} adapter
 * over {@code StoreFacade} — keeping {@code platform-security} free of any dependency on the store module.
 */
@Configuration
public class WebFilterConfig {

	@Bean
	public FilterRegistrationBean<RateLimitFilter> rateLimitFilter() {
		var registration = new FilterRegistrationBean<>(new RateLimitFilter());
		registration.setOrder(10);
		registration.addUrlPatterns("/*");
		return registration;
	}

	@Bean
	public FilterRegistrationBean<TenantBindingFilter> tenantBindingFilter(StoreFacade store) {
		HostTenantResolver hostResolver = host -> store.findByDomain(host).map(ShopView::id);
		var registration = new FilterRegistrationBean<>(
				new TenantBindingFilter(List.of(ApiVersion.CONTROL_PREFIX), hostResolver));
		registration.setOrder(20);
		registration.addUrlPatterns("/*");
		return registration;
	}
}
