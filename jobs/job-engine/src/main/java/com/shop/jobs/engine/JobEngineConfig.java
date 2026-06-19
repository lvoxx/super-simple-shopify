package com.shop.jobs.engine;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on the scheduler that drives {@link OutboxDrainer}. */
@Configuration
@EnableScheduling
public class JobEngineConfig {
}
