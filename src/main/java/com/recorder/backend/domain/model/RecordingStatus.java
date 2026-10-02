package com.recorder.backend.domain.model;

public enum RecordingStatus {
    IDLE,
    CONNECTING,
    RECORDING,
    RECONNECTING,
    COMPLETED,
    FAILED
}
