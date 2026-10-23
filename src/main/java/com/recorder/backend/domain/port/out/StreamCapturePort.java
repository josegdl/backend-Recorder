package com.recorder.backend.domain.port.out;

import com.recorder.backend.domain.model.Channel;
import com.recorder.backend.domain.model.OutputFormat;
import com.recorder.backend.domain.model.StreamProtocol;
import java.io.File;

public interface StreamCapturePort {
    boolean supports(StreamProtocol protocol);
    Process startCapture(Channel channel, OutputFormat format, File outputFile);
    void stopCapture(Long channelId);
}
