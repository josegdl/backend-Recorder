package com.recorder.backend.infrastructure.config;

import com.recorder.backend.domain.model.Channel;
import com.recorder.backend.domain.model.ExclusionRule;
import com.recorder.backend.domain.port.in.RecordingServicePort;
import com.recorder.backend.domain.port.out.ChannelRepositoryPort;
import com.recorder.backend.domain.port.out.StreamSupervisorPort;
import com.recorder.backend.domain.service.RecordingService;
import com.recorder.backend.domain.service.ScheduleWindowParser;
import com.recorder.backend.infrastructure.adapter.out.persistence.InMemoryChannelRepositoryAdapter;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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
            List<ExclusionRule> list1 = new ArrayList<>(toRules(channel.getExclusionList1()));
            List<ExclusionRule> list2 = toRules(channel.getExclusionList2());
            List<ExclusionRule> allowed = toRules(channel.getAllowedWindows());
            if (channel.getScheduleWindow() != null && !channel.getScheduleWindow().isBlank()) {
                list1.addAll(ScheduleWindowParser.parse(channel.getScheduleWindow()));
            }
            int duration = channel.getBlockDurationMinutes() <= 0 ? 60 : channel.getBlockDurationMinutes();
            repository.save(new Channel(channel.getId(), channel.getName(), channel.getSourceUrl(),
                    channel.getProtocol(), channel.isEnabled(), channel.getFormat(), list1, list2, allowed, duration));
        }
        return repository;
    }

    private List<ExclusionRule> toRules(List<RecorderProperties.ExclusionRuleProperties> rules) {
        if (rules == null || rules.isEmpty()) {
            return List.of();
        }
        return rules.stream()
                .filter(r -> r.getStart() != null && r.getEnd() != null)
                .map(r -> new ExclusionRule(r.getDayOfWeek(), r.getStart(), r.getEnd()))
                .collect(Collectors.toList());
    }

    @Bean
    public RecordingServicePort recordingService(ChannelRepositoryPort channelRepository,
                                                 StreamSupervisorPort supervisor,
                                                 RecorderProperties properties) {
        File outputDir = Path.of(properties.getOutputDir()).toAbsolutePath().toFile();
        return new RecordingService(channelRepository, supervisor, outputDir);
    }
}
