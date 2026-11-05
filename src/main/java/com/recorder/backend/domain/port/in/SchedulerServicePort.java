package com.recorder.backend.domain.port.in;

public interface SchedulerServicePort {
    void startScheduler();
    void stopScheduler();
    boolean isRunning();
    void startChannel(Long channelId);
    void stopChannel(Long channelId);
    boolean isChannelActive(Long channelId);
}
