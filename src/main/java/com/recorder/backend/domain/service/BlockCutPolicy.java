package com.recorder.backend.domain.service;

import com.recorder.backend.domain.model.Channel;
import com.recorder.backend.domain.model.ExclusionRule;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Calcula el siguiente corte por bloque respetando duracion por canal,
 * limites de hora en punto y ventanas permitidas/bloqueadas.
 */
public final class BlockCutPolicy {

    private BlockCutPolicy() {
    }

    public static ZonedDateTime nextCut(ZonedDateTime now, Channel channel) {
        if (channel == null) {
            return nextHourBoundary(now);
        }
        int durationMinutes = channel.blockDurationMinutes() <= 0 ? 60 : channel.blockDurationMinutes();
        long durationSeconds = (long) durationMinutes * 60L;

        ZonedDateTime truncatedNow = now.truncatedTo(ChronoUnit.SECONDS);
        long segOfDay = (long) truncatedNow.getHour() * 3600L
                + (long) truncatedNow.getMinute() * 60L
                + (long) truncatedNow.getSecond();
        long rem = segOfDay % durationSeconds;
        long durEmpar = (rem == 0) ? durationSeconds : (durationSeconds - rem);

        ZonedDateTime candidate = truncatedNow.plusSeconds(durEmpar);
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusSeconds(durationSeconds);
        }

        ZonedDateTime windowBoundary = nextWindowBoundary(now, channel);
        if (windowBoundary != null && windowBoundary.isBefore(candidate)) {
            candidate = windowBoundary;
        }
        ZonedDateTime blockedBoundary = nextBlockedBoundary(now, channel);
        if (blockedBoundary != null && blockedBoundary.isBefore(candidate)) {
            candidate = blockedBoundary;
        }
        if (!candidate.isAfter(now)) {
            candidate = now.plusSeconds(Math.max(1, durEmpar));
        }
        return candidate;
    }

    private static ZonedDateTime nextHourBoundary(ZonedDateTime now) {
        return now.truncatedTo(ChronoUnit.HOURS).plusHours(1);
    }

    private static ZonedDateTime nextWindowBoundary(ZonedDateTime now, Channel channel) {
        if (channel.allowedWindows() == null || channel.allowedWindows().isEmpty()) {
            return null;
        }
        ZonedDateTime next = null;
        for (ExclusionRule rule : channel.allowedWindows()) {
            ZonedDateTime end = windowEnd(now, rule);
            if (end != null && end.isAfter(now) && (next == null || end.isBefore(next))) {
                next = end;
            }
        }
        return next;
    }

    private static ZonedDateTime nextBlockedBoundary(ZonedDateTime now, Channel channel) {
        ZonedDateTime next = null;
        next = minBoundary(next, blockedStart(now, channel.exclusionList1()));
        next = minBoundary(next, blockedStart(now, channel.exclusionList2()));
        return next;
    }

    private static ZonedDateTime blockedStart(ZonedDateTime now, java.util.List<ExclusionRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        ZonedDateTime nextCut = null;
        for (ExclusionRule rule : rules) {
            if (rule.start().isBefore(rule.end())) {
                if (HorarioValidator.isOperational(now, new Channel(null, null, null, null, true, null, rules, java.util.List.of(), java.util.List.of(), 60))) {
                    ZonedDateTime start = now.withHour(rule.start().getHour()).withMinute(rule.start().getMinute())
                            .withSecond(0).withNano(0);
                    if (!start.isAfter(now)) {
                        start = start.plusDays(1);
                    }
                    if (rule.dayOfWeek() != null && start.getDayOfWeek() != rule.dayOfWeek()) {
                        start = start.with(s -> s.with(java.time.temporal.TemporalAdjusters.nextOrSame(rule.dayOfWeek())));
                        if (!start.isAfter(now)) {
                            start = start.plusWeeks(1);
                        }
                    }
                    if (nextCut == null || start.isBefore(nextCut)) {
                        nextCut = start;
                    }
                }
            } else if (rule.start().isAfter(rule.end())) {
                boolean inBlock = HorarioValidator.isOperational(now, new Channel(null, null, null, null, true, null, rules, java.util.List.of(), java.util.List.of(), 60)) == false;
                if (inBlock) {
                    return null;
                }
                ZonedDateTime start = now.withHour(rule.start().getHour()).withMinute(rule.start().getMinute())
                        .withSecond(0).withNano(0);
                if (!start.isAfter(now)) {
                    start = start.plusDays(1);
                }
                if (rule.dayOfWeek() != null) {
                    if (now.toLocalTime().isBefore(rule.end())) {
                        start = now.withHour(rule.start().getHour()).withMinute(rule.start().getMinute()).withSecond(0).withNano(0).plusDays(1);
                    }
                }
                if (nextCut == null || start.isBefore(nextCut)) {
                    nextCut = start;
                }
            }
        }
        return nextCut;
    }

    private static ZonedDateTime windowEnd(ZonedDateTime now, ExclusionRule rule) {
        if (rule.start().equals(rule.end())) {
            return null;
        }
        if (rule.start().isBefore(rule.end())) {
            ZonedDateTime end = now.withHour(rule.end().getHour()).withMinute(rule.end().getMinute()).withSecond(0).withNano(0);
            if (now.toLocalTime().isBefore(rule.start())) {
                return null;
            }
            if (end.isBefore(now) || end.equals(now)) {
                end = end.plusDays(1);
            }
            if (rule.dayOfWeek() != null && end.getDayOfWeek() != rule.dayOfWeek()) {
                end = end.with(java.time.temporal.TemporalAdjusters.nextOrSame(rule.dayOfWeek()));
            }
            return end;
        }
        ZonedDateTime end = now.withHour(rule.end().getHour()).withMinute(rule.end().getMinute()).withSecond(0).withNano(0);
        if (now.toLocalTime().isBefore(rule.end())) {
            return end;
        }
        if (now.toLocalTime().isAfter(rule.start())) {
            return end.plusDays(1);
        }
        return null;
    }

    private static ZonedDateTime minBoundary(ZonedDateTime current, ZonedDateTime candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null) {
            return candidate;
        }
        return candidate.isBefore(current) ? candidate : current;
    }
}
