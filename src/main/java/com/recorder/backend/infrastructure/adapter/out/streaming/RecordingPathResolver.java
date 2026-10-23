package com.recorder.backend.infrastructure.adapter.out.streaming;

import com.recorder.backend.domain.model.OutputFormat;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Resuelve la ruta y el nombre del archivo de salida para una grabacion
 * segun el formato:
 * <ul>
 *   <li>MKV: {@code <ruta_base>/<nombre_canal>/<YYYYMMDD>/<nombre_canal>_<YYYYMMDD>_<HHMMSS>.mkv}</li>
 *   <li>MP3: {@code <ruta_base>/<YYYYMMDD>/<nombre_canal>_<YYYYMMDD>_<HHMMSS>.mp3}</li>
 * </ul>
 * Crea los directorios necesarios si no existen y sanitiza el nombre del canal.
 */
public final class RecordingPathResolver {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");

    private RecordingPathResolver() {
    }

    public static File resolveOutputFile(File baseDir, String channelName, OutputFormat format) {
        return resolveOutputFile(baseDir, channelName, format, LocalDateTime.now());
    }

    public static File resolveOutputFile(File baseDir, String channelName, OutputFormat format, LocalDateTime timestamp) {
        String safeChannelName = sanitizeChannelName(channelName);
        String dateFolder = timestamp.format(DATE_FORMATTER);
        String timeStr = timestamp.format(TIME_FORMATTER);

        File targetDir;
        if (format == OutputFormat.MP3) {
            targetDir = new File(baseDir, dateFolder);
        } else {
            targetDir = new File(new File(baseDir, safeChannelName), dateFolder);
        }

        try {
            Files.createDirectories(targetDir.toPath());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear el directorio de grabacion: " + targetDir, e);
        }

        String fileName = safeChannelName + "_" + dateFolder + "_" + timeStr + format.extension();
        return new File(targetDir, fileName);
    }

    public static String sanitizeChannelName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return "canal_desconocido";
        }
        String sanitized = rawName.trim().replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
        return sanitized.isEmpty() ? "canal_desconocido" : sanitized;
    }
}
