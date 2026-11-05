package com.recorder.backend.domain.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Regla de exclusion para el scheduler. Si el instante cae dentro del intervalo
 * definido por la regla, el horario no es valido para operar.
 * <p>
 * Soporta rangos que cruzan medianoche: si {@code start} es posterior a
 * {@code end}, la regla abarca desde {@code start} hasta medianoche y desde
 * medianoche hasta {@code end} del dia siguiente. Si {@code start} y
 * {@code end} son iguales, la regla cubre el dia completo.
 * <p>
 * Si {@code dayOfWeek} es null, la regla aplica a todos los dias.
 */
public record ExclusionRule(
        DayOfWeek dayOfWeek,
        LocalTime start,
        LocalTime end
) {

    public ExclusionRule {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
    }

    public boolean matches(DayOfWeek targetDay, LocalTime targetTime) {
        if (start.equals(end)) {
            return dayOfWeek == null || targetDay == dayOfWeek;
        }
        if (start.isBefore(end)) {
            if (dayOfWeek != null && targetDay != dayOfWeek) {
                return false;
            }
            return !targetTime.isBefore(start) && targetTime.isBefore(end);
        }
        boolean afterStart = !targetTime.isBefore(start);
        boolean beforeEnd = targetTime.isBefore(end);
        if (dayOfWeek == null) {
            return afterStart || beforeEnd;
        }
        if (afterStart) {
            return targetDay == dayOfWeek;
        }
        if (beforeEnd) {
            return targetDay == dayOfWeek.plus(1);
        }
        return false;
    }
}
