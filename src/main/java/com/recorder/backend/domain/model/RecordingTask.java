package com.recorder.backend.domain.model;

import java.time.LocalDateTime;

public record RecordingTask(
    Long id,
    Channel channel,
    LocalDateTime scheduledStart,
    LocalDateTime scheduledEnd,
    String outputFilePath,
    RecordingStatus status
) {}
