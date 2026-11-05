package com.recorder.backend.infrastructure.config;

import com.recorder.backend.domain.port.out.SchedulerPort;
import com.recorder.backend.infrastructure.adapter.out.scheduler.TaskSchedulerAdapter;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RecorderProperties.class)
public class SchedulerConfig {

    @Bean
    public SchedulerPort schedulerPort(RecorderProperties properties) {
        return new TaskSchedulerAdapter(properties.getScheduler().getPoolSize());
    }

    @Bean
    public Clock clock(RecorderProperties properties) {
        String zone = properties.getTimezone();
        if (zone == null || zone.isBlank() || "system".equalsIgnoreCase(zone)) {
            return Clock.systemDefaultZone();
        }
        return Clock.system(ZoneId.of(zone));
    }
}
