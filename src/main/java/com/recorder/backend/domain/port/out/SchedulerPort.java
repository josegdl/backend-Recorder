package com.recorder.backend.domain.port.out;

import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

public interface SchedulerPort {
    ScheduledFuture<?> schedule(Runnable task, Instant triggerTime);
    void shutdownNow();
    void cancelAll();
}
