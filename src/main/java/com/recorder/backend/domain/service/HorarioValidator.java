package com.recorder.backend.domain.service;

import com.recorder.backend.domain.model.Channel;
import com.recorder.backend.domain.model.ExclusionRule;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/**
 * Valida si el instante actual es valido para operar segun dos listas de
 * exclusion configuradas en el canal. Se consideran en union.
 */
public final class HorarioValidator {

    private HorarioValidator() {
    }

    public static boolean isOperational(ZonedDateTime now, Channel channel) {
        if (channel == null) {
            return false;
        }
        if (!channel.enabled()) {
            return false;
        }
        LocalTime time = now.toLocalTime();
        java.util.List<ExclusionRule> allowed = channel.allowedWindows();
        if (allowed != null && !allowed.isEmpty() && !matches(allowed, now, time)) {
            return false;
        }
        if (matches(channel.exclusionList1(), now, time)) {
            return false;
        }
        if (matches(channel.exclusionList2(), now, time)) {
            return false;
        }
        return true;
    }

    private static boolean matches(java.util.List<ExclusionRule> rules, ZonedDateTime now, LocalTime time) {
        if (rules == null || rules.isEmpty()) {
            return false;
        }
        return rules.stream().anyMatch(rule -> rule.matches(now.getDayOfWeek(), time));
    }
}
