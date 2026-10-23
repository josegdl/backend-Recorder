package com.recorder.backend.infrastructure.adapter.out.streaming;

/**
 * Politica de reintentos ante fallos de conexion.
 * {@code maxAttempts = 0} significa reintentos ilimitados.
 */
public record ReconnectPolicy(int maxAttempts, long initialDelayMillis, long maxDelayMillis) {

    public ReconnectPolicy {
        if (maxAttempts < 0) {
            throw new IllegalArgumentException("maxAttempts no puede ser negativo");
        }
        if (initialDelayMillis < 0) {
            throw new IllegalArgumentException("initialDelayMillis no puede ser negativo");
        }
        if (maxDelayMillis < initialDelayMillis) {
            throw new IllegalArgumentException("maxDelayMillis no puede ser menor que initialDelayMillis");
        }
    }

    public boolean isUnlimited() {
        return maxAttempts == 0;
    }

    public boolean exceeded(int consecutiveFailures) {
        return !isUnlimited() && consecutiveFailures >= maxAttempts;
    }

    public long backoffMillis(int consecutiveFailures) {
        long delay = initialDelayMillis;
        int doublings = Math.max(consecutiveFailures - 1, 0);
        for (int i = 0; i < doublings && delay < maxDelayMillis; i++) {
            delay = Math.min(delay * 2, maxDelayMillis);
        }
        return Math.min(delay, maxDelayMillis);
    }
}
