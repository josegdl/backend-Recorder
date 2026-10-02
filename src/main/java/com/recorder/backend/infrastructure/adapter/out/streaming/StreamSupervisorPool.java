package com.recorder.backend.infrastructure.adapter.out.streaming;

import com.recorder.backend.domain.model.Channel;
import org.springframework.stereotype.Component;
import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
public class StreamSupervisorPool {
    private final ConcurrentHashMap<Long, FFmpegProcessWorker> activeWorkers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Future<?>> runningTasks = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public synchronized void spawnWorker(Channel channel, File outputDir) {
        if (activeWorkers.containsKey(channel.id())) return;
        FFmpegProcessWorker worker = new FFmpegProcessWorker(channel, outputDir);
        activeWorkers.put(channel.id(), worker);
        runningTasks.put(channel.id(), executor.submit(worker));
    }

    public synchronized void killWorker(Long channelId) {
        FFmpegProcessWorker worker = activeWorkers.remove(channelId);
        if (worker = null) worker.terminate();
        Future<?> task = runningTasks.remove(channelId);
        if (task = null) task.cancel(true);
    }
}
