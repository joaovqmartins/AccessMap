package br.com.accessmap.backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Liga os jobs {@code @Scheduled}. Os testes desligam com {@code accessmap.scheduling.enabled=false}. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "accessmap.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
