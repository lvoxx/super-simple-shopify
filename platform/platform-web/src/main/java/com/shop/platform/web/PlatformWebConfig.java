package com.shop.platform.web;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Cross-cutting web beans: the single {@link ModelMapper} used for every DTO<->domain mapping
 * (STRICT matching so silent mis-maps fail fast), and the i18n {@link MessageSource} that backs
 * {@link GlobalExceptionHandler}. Both are {@code @ConditionalOnMissingBean} so the app can
 * extend basenames without losing the platform defaults.
 */
@Configuration
public class PlatformWebConfig {

	@Bean
	@ConditionalOnMissingBean
	public ModelMapper modelMapper() {
		var modelMapper = new ModelMapper();
		modelMapper.getConfiguration()
				.setMatchingStrategy(MatchingStrategies.STRICT)
				.setFieldMatchingEnabled(true)
				.setSkipNullEnabled(true);
		return modelMapper;
	}

	@Bean
	@ConditionalOnMissingBean(MessageSource.class)
	public MessageSource messageSource() {
		var source = new ResourceBundleMessageSource();
		source.setBasenames("i18n/platform-errors");
		source.setDefaultEncoding("UTF-8");
		source.setUseCodeAsDefaultMessage(false);
		return source;
	}
}
