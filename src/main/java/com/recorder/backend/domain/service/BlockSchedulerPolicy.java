package com.recorder.backend.domain.service;

import com.recorder.backend.domain.model.BlockTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Calcula bloques horarios de 1 hora.
 * Si se solicita el inicio a mitad de bloque, el bloque empieza en ese instante
 * y termina en la siguiente hora en punto.
 */
public final class BlockSchedulerPolicy {

    private BlockSchedulerPolicy() {
    }

    public static BlockTime currentBlock(ZonedDateTime now) {
        ZonedDateTime start = now.truncatedTo(ChronoUnit.HOURS);
        ZonedDateTime end = start.plusHours(1);
        if (now.isAfter(start)) {
            start = now;
        }
        return new BlockTime(start, end);
    }

    public static ZonedDateTime nextBlockStart(ZonedDateTime currentBlockEnd) {
        return currentBlockEnd.truncatedTo(ChronoUnit.HOURS).plusHours(1);
    }
}
