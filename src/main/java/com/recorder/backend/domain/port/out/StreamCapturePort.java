package com.recorder.backend.domain.port.out;

import com.recorder.backend.domain.model.Channel;
import com.recorder.backend.domain.model.StreamProtocol;
import java.io.File;

public interface StreamCapturePort {
    boolean supports(StreamProtocol protocol);
    Process startCapture(Channel channel, File outputFile);
    void stopCapture(Long channelId);
}
