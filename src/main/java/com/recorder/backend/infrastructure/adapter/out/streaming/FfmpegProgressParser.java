package com.recorder.backend.infrastructure.adapter.out.streaming;

/**
 * Interpreta las lineas que FFmpeg escribe en stdout cuando se invoca con
 * {@code -progress pipe:1}. Una linea de progreso indica que la entrada se
 * abrio y la grabacion esta en curso.
 */
public final class FfmpegProgressParser {

    private static final String PREFIX = "progress=";

    private FfmpegProgressParser() {
    }

    public static boolean signalsRecording(String line) {
        if (line == null) {
            return false;
        }
        String trimmed = line.trim();
        if (!trimmed.startsWith(PREFIX)) {
            return false;
        }
        String value = trimmed.substring(PREFIX.length()).trim();
        return "continue".equals(value) || "end".equals(value);
    }
}
