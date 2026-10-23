package com.recorder.backend.infrastructure.adapter.out.streaming;

import com.recorder.backend.domain.model.Channel;
import com.recorder.backend.domain.model.OutputFormat;
import com.recorder.backend.domain.model.RecordingStatus;
import com.recorder.backend.domain.port.out.StreamCapturePort;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Supervisor de un canal: lanza la captura, informa el estado real de la
 * conexion y reintenta con espera exponencial mientras no se detenga.
 */
public class FFmpegProcessWorker implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(FFmpegProcessWorker.class);

    private final Channel channel;
    private final File outputDir;
    private final OutputFormat format;
    private final StreamCapturePort capturePort;
    private final ReconnectPolicy reconnectPolicy;
    private final Consumer<RecordingStatus> statusListener;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicReference<RecordingStatus> status = new AtomicReference<>(RecordingStatus.IDLE);

    public FFmpegProcessWorker(Channel channel,
                               File outputDir,
                               OutputFormat format,
                               StreamCapturePort capturePort,
                               ReconnectPolicy reconnectPolicy,
                               Consumer<RecordingStatus> statusListener) {
        this.channel = channel;
        this.outputDir = outputDir;
        this.format = format;
        this.capturePort = capturePort;
        this.reconnectPolicy = reconnectPolicy;
        this.statusListener = statusListener;
    }

    @Override
    public void run() {
        int consecutiveFailures = 0;
        try {
            while (running.get()) {
                updateStatus(RecordingStatus.CONNECTING);
                File outputFile = RecordingPathResolver.resolveOutputFile(outputDir, channel.name(), format);
                try {
                    log.info("Iniciando captura del canal {} (intento {}) -> {}",
                            channel.name(), consecutiveFailures + 1, outputFile.getAbsolutePath());
                    Process process = capturePort.startCapture(channel, format, outputFile);
                    if (!running.get()) {
                        capturePort.stopCapture(channel.id());
                        break;
                    }
                    boolean connected = monitorOutput(process);
                    int exitCode = process.waitFor();
                    if (!running.get()) {
                        break;
                    }
                    if (exitCode == 0) {
                        log.info("La captura del canal {} finalizo correctamente", channel.name());
                        updateStatus(RecordingStatus.COMPLETED);
                        break;
                    }
                    if (connected) {
                        log.warn("El canal {} perdio la conexion tras grabar (codigo {}). "
                                + "Se reinicia el conteo de fallos", channel.name(), exitCode);
                        consecutiveFailures = 0;
                    }
                    consecutiveFailures++;
                    if (reconnectPolicy.exceeded(consecutiveFailures)) {
                        log.error("Se agotaron los {} reintentos para el canal {}. Marcando FAILED",
                                reconnectPolicy.maxAttempts(), channel.name());
                        updateStatus(RecordingStatus.FAILED);
                        break;
                    }
                    long delay = reconnectPolicy.backoffMillis(consecutiveFailures);
                    log.warn("FFmpeg finalizo con codigo {} para el canal {}. Reintentando en {} ms",
                            exitCode, channel.name(), delay);
                    updateStatus(RecordingStatus.RECONNECTING);
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException | RuntimeException e) {
                    if (!running.get()) {
                        break;
                    }
                    consecutiveFailures++;
                    log.error("Error al capturar el canal " + channel.name(), e);
                    if (reconnectPolicy.exceeded(consecutiveFailures)) {
                        log.error("Se agotaron los {} reintentos para el canal {}. Marcando FAILED",
                                reconnectPolicy.maxAttempts(), channel.name());
                        updateStatus(RecordingStatus.FAILED);
                        break;
                    }
                    long delay = reconnectPolicy.backoffMillis(consecutiveFailures);
                    log.warn("Reintentando captura del canal {} en {} ms", channel.name(), delay);
                    updateStatus(RecordingStatus.RECONNECTING);
                    if (!sleep(delay)) {
                        break;
                    }
                }
            }
        } finally {
            if (!running.get()) {
                updateStatus(RecordingStatus.IDLE);
            }
            log.info("Captura detenida para el canal {}", channel.name());
        }
    }

    public void terminate() {
        running.set(false);
        capturePort.stopCapture(channel.id());
    }

    public RecordingStatus status() {
        return status.get();
    }

    private boolean monitorOutput(Process process) throws IOException {
        boolean connected = false;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("[{}] {}", channel.name(), line);
                if (!connected && FfmpegProgressParser.signalsRecording(line)) {
                    connected = true;
                    if (running.get()) {
                        updateStatus(RecordingStatus.RECORDING);
                    }
                }
            }
        }
        return connected;
    }

    private void updateStatus(RecordingStatus newStatus) {
        if (!running.get() && newStatus != RecordingStatus.IDLE) {
            return;
        }
        RecordingStatus previous = status.getAndSet(newStatus);
        if (previous != newStatus) {
            statusListener.accept(newStatus);
        }
    }

    private boolean sleep(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
