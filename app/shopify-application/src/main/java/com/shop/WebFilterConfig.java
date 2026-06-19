package com.shop;

import com.shop.platform.security.TenantBindingFilter;
import com.shop.platform.web.RateLimitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the cross-cutting servlet filters in order: rate-limit first (cheap reject before
 * any work), then tenant binding (establishes the {@code ScopedValue} every downstream layer
 * relies on). Both are keyed on the shop header in Phase 0.
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
	public FilterRegistrationBean<TenantBindingFilter> tenantBindingFilter() {
		var registration = new FilterRegistrationBean<>(new TenantBindingFilter());
		registration.setOrder(20);
		registration.addUrlPatterns("/*");
		return registration;
	}
}
