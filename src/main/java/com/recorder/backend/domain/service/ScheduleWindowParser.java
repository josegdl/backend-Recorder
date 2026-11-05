package com.recorder.backend.domain.service;

import com.recorder.backend.domain.model.ExclusionRule;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser para formato compacto de ventana horaria.
 * Ejemplo: {@code 8>2-5} -> todos los dias, excluir/bloquear de 02:00 a 05:00.
 */
public final class ScheduleWindowParser {

    private static final Pattern PATTERN = Pattern.compile("^(\\d+|\\*|ALL)\\s*>\\s*(\\d{1,2})\\s*-\\s*(\\d{1,2})(?::(\\d{2}))?$", Pattern.CASE_INSENSITIVE);

    private ScheduleWindowParser() {
    }

    public static List<ExclusionRule> parse(String window) {
        if (window == null || window.isBlank()) {
            return List.of();
        }
        String normalized = window.trim().replace('–', '-');
        Matcher matcher = PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Formato de ventana no soportado: " + window + " (esperado: 8>2-5)");
        }
        String days = matcher.group(1);
        int startHour = Integer.parseInt(matcher.group(2));
        int endHour = Integer.parseInt(matcher.group(3));
        int startMinute = matcher.group(4) == null ? 0 : Integer.parseInt(matcher.group(4));
        int endMinute = 0;

        LocalTime start = LocalTime.of(Math.floorMod(startHour, 24), Math.floorMod(startMinute, 60));
        LocalTime end = LocalTime.of(Math.floorMod(endHour, 24), Math.floorMod(endMinute, 60));

        boolean allDays = "*".equalsIgnoreCase(days) || "ALL".equalsIgnoreCase(days) || "8".equals(days);
        List<ExclusionRule> rules = new ArrayList<>();
        if (allDays) {
            rules.add(new ExclusionRule(null, start, end));
            return rules;
        }
        try {
            int dayNumber = Integer.parseInt(days);
            if (dayNumber >= 1 && dayNumber <= 7) {
                DayOfWeek day = DayOfWeek.of(dayNumber);
                rules.add(new ExclusionRule(day, start, end));
                return rules;
            }
        } catch (NumberFormatException ignored) {
        }
        rules.add(new ExclusionRule(null, start, end));
        return rules;
    }
}
