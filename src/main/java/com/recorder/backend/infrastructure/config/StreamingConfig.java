package com.recorder.backend.infrastructure.config;

import com.recorder.backend.domain.port.out.StreamCapturePort;
import com.recorder.backend.infrastructure.adapter.out.streaming.FfmpegCommandFactory;
import com.recorder.backend.infrastructure.adapter.out.streaming.ReconnectPolicy;
import com.recorder.backend.infrastructure.adapter.out.streaming.StreamSupervisorPool;
import com.recorder.backend.infrastructure.adapter.out.streaming.YtDlpStreamResolver;
import com.recorder.backend.infrastructure.adapter.out.streaming.adapter.FfmpegStreamCaptureAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registro de beans del subsistema de ingesta.
 * Se declara fuera del dominio para mantenerlo libre de dependencias de Spring.
 */
@Configuration
public class StreamingConfig {

    @Bean
    public FfmpegCommandFactory ffmpegCommandFactory(
            @Value("${recorder.ffmpeg.path:ffmpeg}") String ffmpegExecutable) {
        return new FfmpegCommandFactory(ffmpegExecutable);
    }

    @Bean
    public YtDlpStreamResolver ytDlpStreamResolver(
            @Value("${recorder.yt-dlp.path:yt-dlp}") String ytDlpExecutable) {
        return new YtDlpStreamResolver(ytDlpExecutable);
    }

    @Bean
    public StreamCapturePort streamCapturePort(FfmpegCommandFactory commandFactory,
                                               YtDlpStreamResolver streamResolver) {
        return new FfmpegStreamCaptureAdapter(commandFactory, streamResolver);
    }

    @Bean
    public StreamSupervisorPool streamSupervisorPool(StreamCapturePort streamCapturePort,
                                                     RecorderProperties properties) {
        RecorderProperties.Reconnect reconnect = properties.getReconnect();
        ReconnectPolicy policy = new ReconnectPolicy(
                reconnect.getMaxAttempts(),
                reconnect.getInitialDelay().toMillis(),
                reconnect.getMaxDelay().toMillis());
        return new StreamSupervisorPool(streamCapturePort, policy);
    }
}
