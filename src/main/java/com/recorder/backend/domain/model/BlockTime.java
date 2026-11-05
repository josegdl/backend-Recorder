package com.recorder.backend.domain.model;

import java.time.ZonedDateTime;

/**
 * Representa un bloque horario de 1 hora para la grabacion.
 */
public record BlockTime(
        ZonedDateTime start,
        ZonedDateTime end
) {
}
