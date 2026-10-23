package com.recorder.backend.infrastructure.config;

import com.recorder.backend.domain.model.Channel;
import com.recorder.backend.domain.port.in.RecordingServicePort;
import com.recorder.backend.domain.port.out.ChannelRepositoryPort;
import com.recorder.backend.domain.port.out.StreamSupervisorPort;
import com.recorder.backend.domain.service.RecordingService;
import com.recorder.backend.infrastructure.adapter.out.persistence.InMemoryChannelRepositoryAdapter;
import java.io.File;
import java.nio.file.Path;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registro de beans del caso de uso de grabacion.
 */
@Configuration
@EnableConfigurationProperties(RecorderProperties.class)
public class RecordingConfig {

    @Bean
    public ChannelRepositoryPort channelRepository(RecorderProperties properties) {
        InMemoryChannelRepositoryAdapter repository = new InMemoryChannelRepositoryAdapter();
        for (RecorderProperties.ChannelProperties channel : properties.getChannels()) {
            repository.save(new Channel(channel.getId(), channel.getName(), channel.getSourceUrl(),
                    channel.getProtocol(), channel.isEnabled(), channel.getFormat()));
        }
        return repository;
    }

    @Bean
    public RecordingServicePort recordingService(ChannelRepositoryPort channelRepository,
                                                 StreamSupervisorPort supervisor,
                                                 RecorderProperties properties) {
        File outputDir = Path.of(properties.getOutputDir()).toAbsolutePath().toFile();
        return new RecordingService(channelRepository, supervisor, outputDir);
    }
}
