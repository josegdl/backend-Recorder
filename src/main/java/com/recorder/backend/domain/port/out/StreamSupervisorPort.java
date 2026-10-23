package com.recorder.backend.domain.port.out;

import com.recorder.backend.domain.model.Channel;
import com.recorder.backend.domain.model.OutputFormat;
import com.recorder.backend.domain.model.RecordingStatus;
import java.io.File;

public interface StreamSupervisorPort {
    void start(Channel channel, OutputFormat format, File outputDir);
    void stop(Long channelId);
    boolean isRunning(Long channelId);
    RecordingStatus status(Long channelId);
}
