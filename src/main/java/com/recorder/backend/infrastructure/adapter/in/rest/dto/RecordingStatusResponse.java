package com.recorder.backend.infrastructure.adapter.in.rest.dto;

import com.recorder.backend.domain.model.RecordingStatus;

public record RecordingStatusResponse(Long channelId, RecordingStatus status) {
}
