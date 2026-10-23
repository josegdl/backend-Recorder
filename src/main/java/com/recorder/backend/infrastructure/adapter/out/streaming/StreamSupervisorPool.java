package com.recorder.backend.infrastructure.adapter.out.streaming;

import com.recorder.backend.domain.model.Channel;
import com.recorder.backend.domain.model.OutputFormat;
import com.recorder.backend.domain.model.RecordingStatus;
import com.recorder.backend.domain.port.out.StreamCapturePort;
import com.recorder.backend.domain.port.out.StreamSupervisorPort;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mantiene un hilo independiente por canal, publica el estado real de cada
 * captura y garantiza que solo exista un supervisor activo por canal.
 */
public class StreamSupervisorPool implements StreamSupervisorPort {

    private static final Logger log = LoggerFactory.getLogger(StreamSupervisorPool.class);

    private final StreamCapturePort capturePort;
    private final ReconnectPolicy reconnectPolicy;
    private final ConcurrentHashMap<Long, FFmpegProcessWorker> activeWorkers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Future<?>> runningTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, RecordingStatus> statuses = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public StreamSupervisorPool(StreamCapturePort capturePort, ReconnectPolicy reconnectPolicy) {
        this.capturePort = capturePort;
        this.reconnectPolicy = reconnectPolicy;
    }

    @Override
    public synchronized void start(Channel channel, OutputFormat format, File outputDir) {
        if (isRunning(channel.id())) {
            log.debug("El canal {} ya tiene un supervisor activo", channel.id());
            return;
        }
        statuses.put(channel.id(), RecordingStatus.CONNECTING);
        FFmpegProcessWorker worker = new FFmpegProcessWorker(channel, outputDir, format, capturePort,
                reconnectPolicy, newStatus -> statuses.put(channel.id(), newStatus));
        activeWorkers.put(channel.id(), worker);
        runningTasks.put(channel.id(), executor.submit(() -> {
            Thread.currentThread().setName("recording-channel-" + channel.id());
            worker.run();
        }));
        log.info("Supervisor iniciado para el canal {}", channel.id());
    }

    @Override
    public synchronized void stop(Long channelId) {
        FFmpegProcessWorker worker = activeWorkers.remove(channelId);
        if (worker != null) {
            worker.terminate();
        }
        Future<?> task = runningTasks.remove(channelId);
        if (task != null) {
            task.cancel(true);
        }
        statuses.put(channelId, RecordingStatus.IDLE);
        log.info("Supervisor detenido para el canal {}", channelId);
    }

    @Override
    public boolean isRunning(Long channelId) {
        Future<?> task = runningTasks.get(channelId);
        return task != null && !task.isDone();
    }

    @Override
    public RecordingStatus status(Long channelId) {
        return statuses.getOrDefault(channelId, RecordingStatus.IDLE);
    }

    @PreDestroy
    public synchronized void shutdown() {
        activeWorkers.keySet().forEach(this::stop);
        executor.shutdownNow();
    }
}
