package com.recorder.backend.infrastructure.adapter.out.streaming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.recorder.backend.domain.model.OutputFormat;
import java.io.File;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class RecordingPathResolverTest {

    @Test
    void generaRutaEspecialParaMkvConCanalYFecha() {
        File baseDir = new File("recordings");
        LocalDateTime timestamp = LocalDateTime.of(2026, 10, 19, 14, 30, 45);

        File outputFile = RecordingPathResolver.resolveOutputFile(baseDir, "Canal 1 HD", OutputFormat.MKV, timestamp);

        String expectedRelativePath = "recordings" + File.separator + "Canal_1_HD" + File.separator + "20261019" + File.separator + "Canal_1_HD_20261019_143045.mkv";
        assertTrue(outputFile.getPath().endsWith(expectedRelativePath));
    }

    @Test
    void generaRutaEspecialParaMp3SoloConFecha() {
        File baseDir = new File("recordings");
        LocalDateTime timestamp = LocalDateTime.of(2026, 10, 19, 14, 30, 45);

        File outputFile = RecordingPathResolver.resolveOutputFile(baseDir, "Canal 1 HD", OutputFormat.MP3, timestamp);

        String expectedRelativePath = "recordings" + File.separator + "20261019" + File.separator + "Canal_1_HD_20261019_143045.mp3";
        assertTrue(outputFile.getPath().endsWith(expectedRelativePath));
    }

    @Test
    void sanitizaNombresDeCanalesConCaracteresEspeciales() {
        assertEquals("Canal_Noticias", RecordingPathResolver.sanitizeChannelName("Canal / Noticias *"));
        assertEquals("canal_desconocido", RecordingPathResolver.sanitizeChannelName("   "));
        assertEquals("canal_desconocido", RecordingPathResolver.sanitizeChannelName(null));
    }
}
