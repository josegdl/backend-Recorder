package com.recorder.backend.infrastructure.adapter.out.streaming;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FfmpegProgressParserTest {

    @Test
    void detectaProgresoContinuo() {
        assertTrue(FfmpegProgressParser.signalsRecording("progress=continue"));
        assertTrue(FfmpegProgressParser.signalsRecording("  progress=continue  "));
    }

    @Test
    void detectaFinDeProgreso() {
        assertTrue(FfmpegProgressParser.signalsRecording("progress=end"));
    }

    @Test
    void ignoraLineasSinSenalDeGrabacion() {
        assertFalse(FfmpegProgressParser.signalsRecording("frame=120"));
        assertFalse(FfmpegProgressParser.signalsRecording("progress=cont"));
        assertFalse(FfmpegProgressParser.signalsRecording(""));
        assertFalse(FfmpegProgressParser.signalsRecording(null));
    }
}
