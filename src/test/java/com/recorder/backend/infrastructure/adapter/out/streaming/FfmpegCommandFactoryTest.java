package com.recorder.backend.infrastructure.adapter.out.streaming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.recorder.backend.domain.model.Channel;
import com.recorder.backend.domain.model.OutputFormat;
import com.recorder.backend.domain.model.StreamProtocol;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;

class FfmpegCommandFactoryTest {

    private final FfmpegCommandFactory factory = new FfmpegCommandFactory("ffmpeg");

    private Channel channel(StreamProtocol protocol) {
        return new Channel(1L, "Canal", "http://example.test/stream", protocol, true, com.recorder.backend.domain.model.OutputFormat.MKV, java.util.List.of(), java.util.List.of(), java.util.List.of(), 60);
    }

    @Test
    void generaSalidaMkvCopiandoElFlujoParaHls() {
        List<String> command = factory.buildCaptureCommand(
                channel(StreamProtocol.M3U8_HLS), OutputFormat.MKV,
                "http://example.test/stream.m3u8", new File("salida.mkv"));

        assertEquals("ffmpeg", command.get(0));
        assertTrue(command.contains("-reconnect"), "HLS debe habilitar reconexión HTTP");
        assertTrue(command.contains("-threads"));
        assertTrue(command.contains("-drop_pkts_on_overflow"));
        assertTrue(command.contains("-use_wallclock_as_timestamps"));
        assertTrue(indexOf(command, "-c") + 1 == indexOf(command, "copy"));
        assertTrue(command.contains("matroska"));
        assertTrue(command.get(command.size() - 1).endsWith("salida.mkv"));
    }

    @Test
    void generaSalidaMp3ExtrayendoAudio() {
        List<String> command = factory.buildCaptureCommand(
                channel(StreamProtocol.RTMP), OutputFormat.MP3,
                "rtmp://example.test/live", new File("salida.mp3"));

        assertTrue(command.contains("-vn"));
        assertTrue(command.contains("libmp3lame"));
        assertTrue(command.contains("mp3"));
    }

    @Test
    void noAnadeReconexionEnRtmpNiTuner() {
        List<String> rtmp = factory.buildCaptureCommand(
                channel(StreamProtocol.RTMP), OutputFormat.MKV,
                "rtmp://example.test/live", new File("salida.mkv"));
        List<String> tuner = factory.buildCaptureCommand(
                channel(StreamProtocol.TV_TUNER), OutputFormat.MKV,
                "file:///dev/video0", new File("salida.mkv"));

        assertFalse(rtmp.contains("-reconnect"));
        assertFalse(tuner.contains("-reconnect"));
    }

    @Test
    void anadeReconexionEnYtDlpResueltas() {
        List<String> command = factory.buildCaptureCommand(
                channel(StreamProtocol.YOUTUBE), OutputFormat.MKV,
                "https://rr1.googlevideo.com/videoplayback", new File("salida.mkv"));

        assertTrue(command.contains("-reconnect"));
    }

    @Test
    void solicitaProgresoPorStdoutParaDetectarGrabacion() {
        List<String> command = factory.buildCaptureCommand(
                channel(StreamProtocol.M3U8_HLS), OutputFormat.MKV,
                "http://example.test/stream.m3u8", new File("salida.mkv"));

        int index = command.indexOf("-progress");
        assertTrue(index >= 0, "Debe solicitar progreso a FFmpeg");
        assertEquals("pipe:1", command.get(index + 1));
        assertTrue(command.contains("-nostats"));
    }

    @Test
    void rechazaUrlVaciaYFormatoNulo() {
        assertThrows(IllegalArgumentException.class, () -> factory.buildCaptureCommand(
                channel(StreamProtocol.M3U8_HLS), OutputFormat.MKV, "  ", new File("salida.mkv")));
        assertThrows(IllegalArgumentException.class, () -> factory.buildCaptureCommand(
                channel(StreamProtocol.M3U8_HLS), null, "http://example.test/s.m3u8", new File("salida.mkv")));
    }

    private int indexOf(List<String> command, String token) {
        return command.indexOf(token);
    }
}
