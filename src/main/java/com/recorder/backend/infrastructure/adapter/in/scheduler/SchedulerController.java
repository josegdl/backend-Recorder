package com.recorder.backend.infrastructure.adapter.in.scheduler;

import com.recorder.backend.domain.port.in.SchedulerServicePort;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/scheduler")
public class SchedulerController {

    private final SchedulerServicePort schedulerService;

    public SchedulerController(SchedulerServicePort schedulerService) {
        this.schedulerService = schedulerService;
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void start() {
        schedulerService.startScheduler();
    }

    @PostMapping("/stop")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void stop() {
        schedulerService.stopScheduler();
    }

    @GetMapping("/status")
    public Map<String, Boolean> status() {
        return Map.of("running", schedulerService.isRunning());
    }
}
