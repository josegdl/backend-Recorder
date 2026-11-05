package com.recorder.backend.infrastructure.adapter.out.scheduler;

import com.recorder.backend.domain.port.out.SchedulerPort;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Adaptador reutilizable sobre ThreadPoolTaskScheduler. Tras un apagado
 * ordenado crea una instancia nueva cuando se vuelve a programar.
 */
public class TaskSchedulerAdapter implements SchedulerPort {

    private final int poolSize;
    private final Set<ScheduledFuture<?>> tasks = ConcurrentHashMap.newKeySet();
    private ThreadPoolTaskScheduler scheduler;

    public TaskSchedulerAdapter(int poolSize) {
        if (poolSize <= 0) {
            throw new IllegalArgumentException("El tamano del pool del scheduler debe ser positivo");
        }
        this.poolSize = poolSize;
    }

    @Override
    public synchronized ScheduledFuture<?> schedule(Runnable task, Instant triggerTime) {
        ensureStarted();
        AtomicReference<ScheduledFuture<?>> reference = new AtomicReference<>();
        Runnable tracked = () -> {
            try {
                task.run();
            } finally {
                ScheduledFuture<?> current = reference.get();
                if (current != null) {
                    tasks.remove(current);
                }
            }
        };
        ScheduledFuture<?> future = scheduler.schedule(tracked, triggerTime);
        if (future != null) {
            reference.set(future);
            tasks.add(future);
            if (future.isDone()) {
                tasks.remove(future);
            }
        }
        return future;
    }

    @Override
    public synchronized void shutdownNow() {
        cancelAll();
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
    }

    @Override
    public synchronized void cancelAll() {
        tasks.forEach(future -> future.cancel(true));
        tasks.clear();
    }

    private void ensureStarted() {
        if (scheduler != null && !scheduler.getScheduledThreadPoolExecutor().isShutdown()) {
            return;
        }
        ThreadPoolTaskScheduler newScheduler = new ThreadPoolTaskScheduler();
        newScheduler.setPoolSize(poolSize);
        newScheduler.setThreadNamePrefix("recorder-scheduler-");
        newScheduler.setAwaitTerminationSeconds(10);
        newScheduler.setWaitForTasksToCompleteOnShutdown(true);
        newScheduler.initialize();
        scheduler = newScheduler;
    }
}
