package com.recorder.backend.infrastructure.adapter.in.rest;

import com.recorder.backend.domain.port.in.RecordingServicePort;
import com.recorder.backend.infrastructure.adapter.in.rest.dto.RecordingStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/monitoring")
public class MonitorController {

    private final RecordingServicePort recordingService;

    public MonitorController(RecordingServicePort recordingService) {
        this.recordingService = recordingService;
    }

    @GetMapping("/{id}/status")
    public RecordingStatusResponse status(@PathVariable Long id) {
        return new RecordingStatusResponse(id, recordingService.getChannelStatus(id));
    }
}
