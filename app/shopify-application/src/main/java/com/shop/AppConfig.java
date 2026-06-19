package com.shop;

import com.shop.platform.core.SystemTimeProvider;
import com.shop.platform.core.TimeProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** App-level beans that adapt pure platform-core types into the Spring context. */
@Configuration
public class AppConfig {

	@Bean
	public TimeProvider timeProvider() {
		return new SystemTimeProvider();
	}
}
