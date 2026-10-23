package com.recorder.backend.infrastructure.adapter.out.streaming.adapter;

import com.recorder.backend.domain.model.Channel;
import com.recorder.backend.domain.model.OutputFormat;
import com.recorder.backend.domain.model.StreamProtocol;
import com.recorder.backend.domain.port.out.StreamCapturePort;
import com.recorder.backend.infrastructure.adapter.out.streaming.FfmpegCommandFactory;
import com.recorder.backend.infrastructure.adapter.out.streaming.YtDlpStreamResolver;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adaptador de ingesta basado en FFmpeg como proceso externo.
 * Para YouTube y Facebook obtiene primero la URL directa mediante yt-dlp.
 */
public class FfmpegStreamCaptureAdapter implements StreamCapturePort {

    private static final Logger log = LoggerFactory.getLogger(FfmpegStreamCaptureAdapter.class);

    private final FfmpegCommandFactory commandFactory;
    private final YtDlpStreamResolver streamResolver;
    private final Map<Long, Process> activeProcesses = new ConcurrentHashMap<>();

    public FfmpegStreamCaptureAdapter(FfmpegCommandFactory commandFactory, YtDlpStreamResolver streamResolver) {
        this.commandFactory = commandFactory;
        this.streamResolver = streamResolver;
    }

    @Override
    public boolean supports(StreamProtocol protocol) {
        return protocol != null;
    }

    @Override
    public Process startCapture(Channel channel, OutputFormat format, File outputFile) {
        String sourceUrl = resolveSourceUrl(channel);
        List<String> command = commandFactory.buildCaptureCommand(channel, format, sourceUrl, outputFile);
        log.debug("Comando de captura para el canal {}: {}", channel.id(), command);

        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            activeProcesses.put(channel.id(), process);
            return process;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo iniciar FFmpeg para el canal " + channel.id(), e);
        }
    }

    @Override
    public void stopCapture(Long channelId) {
        Process process = activeProcesses.remove(channelId);
        if (process != null && process.isAlive()) {
            process.destroy();
            log.info("Proceso de FFmpeg detenido para el canal {}", channelId);
        }
    }

    private String resolveSourceUrl(Channel channel) {
        if (requiresYtDlp(channel.protocol())) {
            return streamResolver.resolve(channel.sourceUrl());
        }
        return channel.sourceUrl();
    }

    private boolean requiresYtDlp(StreamProtocol protocol) {
        return protocol == StreamProtocol.YOUTUBE || protocol == StreamProtocol.FACEBOOK;
    }
}
