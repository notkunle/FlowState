package com.pm.flowstate.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

// lets the slow Gemini call run off the scheduler thread, so vitals keep flowing every second
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
}