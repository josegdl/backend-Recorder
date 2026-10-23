package com.recorder.backend.domain.model;

import java.time.LocalDateTime;

public class RecordingTask {
    private final Long id;
    private final Channel channel;
    private final LocalDateTime scheduledStart;
    private final LocalDateTime scheduledEnd;
    private final String outputFilePath;
    private final RecordingStatus status;

    public RecordingTask(Long id, Channel channel, LocalDateTime scheduledStart, LocalDateTime scheduledEnd, String outputFilePath, RecordingStatus status) {
        this.id = id;
        this.channel = channel;
        this.scheduledStart = scheduledStart;
        this.scheduledEnd = scheduledEnd;
        this.outputFilePath = outputFilePath;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Channel getChannel() {
        return channel;
    }

    public LocalDateTime getScheduledStart() {
        return scheduledStart;
    }

    public LocalDateTime getScheduledEnd() {
        return scheduledEnd;
    }

    public String getOutputFilePath() {
        return outputFilePath;
    }

    public RecordingStatus getStatus() {
        return status;
    }
}
