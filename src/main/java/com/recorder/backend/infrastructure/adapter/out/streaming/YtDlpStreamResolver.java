package com.recorder.backend.infrastructure.adapter.out.streaming;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resuelve la URL directa de un stream usando yt-dlp.
 * Se emplea para las fuentes que no exponen una URL reproducible directa (YouTube, Facebook).
 */
public class YtDlpStreamResolver {

    private static final Logger log = LoggerFactory.getLogger(YtDlpStreamResolver.class);
    private static final String HTTP_PREFIX = "http";

    private final String ytDlpExecutable;

    public YtDlpStreamResolver(String ytDlpExecutable) {
        if (ytDlpExecutable == null || ytDlpExecutable.isBlank()) {
            throw new IllegalArgumentException("La ruta del ejecutable de yt-dlp es obligatoria");
        }
        this.ytDlpExecutable = ytDlpExecutable;
    }

    public String resolve(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException("La URL de origen es obligatoria para yt-dlp");
        }

        try {
            ProcessBuilder builder = new ProcessBuilder(ytDlpExecutable, "-g", "-f", "best", sourceUrl);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            String resolvedUrl = readFirstHttpUrl(process);
            int exitCode = process.waitFor();
            if (resolvedUrl == null || exitCode != 0) {
                throw new IllegalStateException(
                        "yt-dlp no pudo resolver la URL del stream (codigo de salida " + exitCode + ")");
            }
            log.debug("yt-dlp resolvio la URL del stream para {}", sourceUrl);
            return resolvedUrl;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo ejecutar yt-dlp: " + ytDlpExecutable, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La resolucion con yt-dlp fue interrumpida", e);
        }
    }

    private String readFirstHttpUrl(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String candidate = line.trim();
                if (candidate.startsWith(HTTP_PREFIX)) {
                    return candidate;
                }
            }
        }
        return null;
    }
}
