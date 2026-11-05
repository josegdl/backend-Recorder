package com.recorder.backend.infrastructure.adapter.in.scheduler;

import com.recorder.backend.domain.port.in.SchedulerServicePort;
import com.recorder.backend.infrastructure.config.RecorderProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Arranca el scheduler automaticamente al iniciar la aplicacion cuando
 * {@code recorder.scheduler.enabled=true}.
 */
@Component
public class SchedulerBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchedulerBootstrap.class);

    private final SchedulerServicePort schedulerService;
    private final RecorderProperties properties;

    public SchedulerBootstrap(SchedulerServicePort schedulerService, RecorderProperties properties) {
        this.schedulerService = schedulerService;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.getScheduler().isEnabled()) {
            log.info("Scheduler deshabilitado (recorder.scheduler.enabled=false)");
            return;
        }
        log.info("Auto-arranque del scheduler habilitado");
        schedulerService.startScheduler();
    }
}
