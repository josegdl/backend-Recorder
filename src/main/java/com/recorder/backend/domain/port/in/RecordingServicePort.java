package com.recorder.backend.domain.port.in;

import com.recorder.backend.domain.model.RecordingStatus;

public interface RecordingServicePort {
    void startChannelRecording(Long channelId);
    void stopChannelRecording(Long channelId);
    RecordingStatus getChannelStatus(Long channelId);
}
