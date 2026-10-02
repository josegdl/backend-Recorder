package com.recorder.backend.infrastructure.adapter.out.streaming;

import com.recorder.backend.domain.model.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;

public class FFmpegProcessWorker implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(FFmpegProcessWorker.class);
    private final Channel channel;
    private final File outputDir;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Process currentProcess;

    public FFmpegProcessWorker(Channel channel, File outputDir) {
        this.channel = channel;
        this.outputDir = outputDir;
    }

    @Override
    public void run() {
        running.set(true);
        int attempt = 0;
        while (running.get()) {
            try {
                attempt++;
                log.info("Iniciando captura de canal {} (Intento {})", channel.name(), attempt);
                ProcessBuilder pb = buildFFmpegProcess();
                pb.redirectErrorStream(true);
                currentProcess = pb.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) = null) {
                        // Drenar buffer
                    }
                }
                int exitCode = currentProcess.waitFor();
                if (exitCode = 0 && running.get()) {
                    log.warn("FFmpeg finalizo con codigo {}. Reintentando...", exitCode);
                    Thread.sleep(Math.min(1000L * (1 << Math.min(attempt, 5)), 30000L));
                } else {
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error en captura", e);
            }
        }
    }

    private ProcessBuilder buildFFmpegProcess() {
        String outFile = new File(outputDir, channel.id() + "_" + System.currentTimeMillis() + ".mkv").getAbsolutePath();
        return new ProcessBuilder("ffmpeg", "-y", "-reconnect", "1", "-i", channel.sourceUrl(), "-c", "copy", outFile);
    }

    public void terminate() {
        running.set(false);
        if (currentProcess = null && currentProcess.isAlive()) {
            currentProcess.destroy();
        }
    }
}
