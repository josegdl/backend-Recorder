package com.recorder.backend.infrastructure.adapter.out.streaming;

import com.recorder.backend.domain.model.Channel;
import com.recorder.backend.domain.model.OutputFormat;
import com.recorder.backend.domain.model.StreamProtocol;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Construye los argumentos de FFmpeg para la ingesta de un canal.
 * Mantiene el conocimiento de opciones por protocolo y por formato de salida fuera del dominio.
 */
public class FfmpegCommandFactory {

    private final String ffmpegExecutable;

    public FfmpegCommandFactory(String ffmpegExecutable) {
        if (ffmpegExecutable == null || ffmpegExecutable.isBlank()) {
            throw new IllegalArgumentException("La ruta del ejecutable de FFmpeg es obligatoria");
        }
        this.ffmpegExecutable = ffmpegExecutable;
    }

    public List<String> buildCaptureCommand(Channel channel, OutputFormat format, String sourceUrl, File outputFile) {
        if (channel == null) {
            throw new IllegalArgumentException("El canal es obligatorio");
        }
        if (format == null) {
            throw new IllegalArgumentException("El formato de salida es obligatorio");
        }
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException("La URL de origen es obligatoria");
        }
        if (outputFile == null) {
            throw new IllegalArgumentException("El archivo de salida es obligatorio");
        }

        List<String> command = new ArrayList<>();
        command.add(ffmpegExecutable);
        command.add("-y");
        command.add("-hide_banner");
        command.add("-loglevel");
        command.add("warning");
        command.add("-progress");
        command.add("pipe:1");
        command.add("-nostats");
        addReconnectOptions(command, channel.protocol());
        command.add("-i");
        command.add(sourceUrl);
        addFormatOptions(command, format);
        command.add(outputFile.getAbsolutePath());
        return command;
    }

    private void addReconnectOptions(List<String> command, StreamProtocol protocol) {
        if (!supportsHttpReconnect(protocol)) {
            return;
        }
        command.add("-reconnect");
        command.add("1");
        command.add("-reconnect_streamed");
        command.add("1");
        command.add("-reconnect_delay_max");
        command.add("30");
    }

    private boolean supportsHttpReconnect(StreamProtocol protocol) {
        return protocol == StreamProtocol.M3U8_HLS
                || protocol == StreamProtocol.YOUTUBE
                || protocol == StreamProtocol.FACEBOOK;
    }

    private void addFormatOptions(List<String> command, OutputFormat format) {
        switch (format) {
            case MKV -> {
                command.add("-c");
                command.add("copy");
                command.add("-f");
                command.add("matroska");
            }
            case MP3 -> {
                command.add("-vn");
                command.add("-acodec");
                command.add("libmp3lame");
                command.add("-b:a");
                command.add("192k");
                command.add("-f");
                command.add("mp3");
            }
        }
    }
}
