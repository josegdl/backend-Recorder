package com.recorder.backend.infrastructure.adapter.in.rest;

import com.recorder.backend.domain.port.in.SchedulerServicePort;
import com.recorder.backend.domain.port.out.ChannelRepositoryPort;
import com.recorder.backend.infrastructure.adapter.in.rest.dto.ChannelResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/channels")
public class ChannelController {

    private final ChannelRepositoryPort channelRepository;
    private final SchedulerServicePort schedulerService;

    public ChannelController(ChannelRepositoryPort channelRepository,
                             SchedulerServicePort schedulerService) {
        this.channelRepository = channelRepository;
        this.schedulerService = schedulerService;
    }

    @GetMapping
    public List<ChannelResponse> list() {
        return channelRepository.findAll().stream()
                .map(ChannelResponse::from)
                .toList();
    }

    @PostMapping("/{id}/start")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void start(@PathVariable Long id) {
        schedulerService.startChannel(id);
    }

    @PostMapping("/{id}/stop")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void stop(@PathVariable Long id) {
        schedulerService.stopChannel(id);
    }
}
