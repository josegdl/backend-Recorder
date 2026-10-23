package com.recorder.backend.domain.service;

import com.recorder.backend.domain.exception.ChannelNotFoundException;
import com.recorder.backend.domain.model.Channel;
import com.recorder.backend.domain.model.RecordingStatus;
import com.recorder.backend.domain.port.in.RecordingServicePort;
import com.recorder.backend.domain.port.out.ChannelRepositoryPort;
import com.recorder.backend.domain.port.out.StreamSupervisorPort;
import java.io.File;
import java.util.Objects;

/**
 * Caso de uso de grabacion: cada canal se ejecuta en su propio supervisor
 * y puede arrancarse o detenerse de forma independiente.
 */
public class RecordingService implements RecordingServicePort {

    private final ChannelRepositoryPort channelRepository;
    private final StreamSupervisorPort supervisor;
    private final File outputDir;

    public RecordingService(ChannelRepositoryPort channelRepository,
                            StreamSupervisorPort supervisor,
                            File outputDir) {
        this.channelRepository = Objects.requireNonNull(channelRepository, "channelRepository");
        this.supervisor = Objects.requireNonNull(supervisor, "supervisor");
        this.outputDir = Objects.requireNonNull(outputDir, "outputDir");
    }

    @Override
    public void startChannelRecording(Long channelId) {
        Channel channel = findChannel(channelId);
        if (!channel.enabled()) {
            throw new IllegalStateException("El canal " + channelId + " esta deshabilitado");
        }
        if (supervisor.isRunning(channelId)) {
            return;
        }
        supervisor.start(channel, channel.format(), outputDir);
    }

    @Override
    public void stopChannelRecording(Long channelId) {
        findChannel(channelId);
        supervisor.stop(channelId);
    }

    @Override
    public RecordingStatus getChannelStatus(Long channelId) {
        findChannel(channelId);
        return supervisor.status(channelId);
    }

    private Channel findChannel(Long channelId) {
        if (channelId == null) {
            throw new IllegalArgumentException("El id del canal es obligatorio");
        }
        return channelRepository.findById(channelId)
                .orElseThrow(() -> new ChannelNotFoundException(channelId));
    }
}
