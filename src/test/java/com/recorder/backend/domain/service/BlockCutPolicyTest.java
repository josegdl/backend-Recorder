package com.recorder.backend.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.recorder.backend.domain.model.Channel;
import com.recorder.backend.domain.model.ExclusionRule;
import com.recorder.backend.domain.model.OutputFormat;
import com.recorder.backend.domain.model.StreamProtocol;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class BlockCutPolicyTest {

    private final ZoneId zone = ZoneId.systemDefault();

    private Channel canalConDuracion(int minutos) {
        return new Channel(
                1L, "ADN40", "http://example.test/stream.m3u8",
                StreamProtocol.M3U8_HLS, true, OutputFormat.MKV,
                List.of(), List.of(), List.of(), minutos
        );
    }

    @Test
    void emparejaBloqueDe10MinutosDesdeMitadDeIntervalo() {
        // Si arranca a las 18:07:40 con duracion de 10 minutos,
        // debe emparejar y cortar exactamente a las 18:10:00.
        ZonedDateTime now = ZonedDateTime.of(2026, 10, 27, 18, 7, 40, 0, zone);
        ZonedDateTime expected = ZonedDateTime.of(2026, 10, 27, 18, 10, 0, 0, zone);

        ZonedDateTime cut = BlockCutPolicy.nextCut(now, canalConDuracion(10));

        assertEquals(expected, cut);
    }

    @Test
    void calculaBloqueCompletoDe10MinutosCuandoIniciaEnPunto() {
        // Si arranca exactamente a las 18:10:00 con duracion de 10 minutos,
        // el siguiente bloque completo debe cortar a las 18:20:00.
        ZonedDateTime now = ZonedDateTime.of(2026, 10, 27, 18, 10, 0, 0, zone);
        ZonedDateTime expected = ZonedDateTime.of(2026, 10, 27, 18, 20, 0, 0, zone);

        ZonedDateTime cut = BlockCutPolicy.nextCut(now, canalConDuracion(10));

        assertEquals(expected, cut);
    }

    @Test
    void emparejaBloqueDe60Minutos() {
        // Con 60 minutos arrancando a las 18:07:40, debe cortar a las 19:00:00.
        ZonedDateTime now = ZonedDateTime.of(2026, 10, 27, 18, 7, 40, 0, zone);
        ZonedDateTime expected = ZonedDateTime.of(2026, 10, 27, 19, 0, 0, 0, zone);

        ZonedDateTime cut = BlockCutPolicy.nextCut(now, canalConDuracion(60));

        assertEquals(expected, cut);
    }

    @Test
    void emparejaBloqueDe15Minutos() {
        // Con 15 minutos arrancando a las 18:07:40, debe cortar a las 18:15:00.
        ZonedDateTime now = ZonedDateTime.of(2026, 10, 27, 18, 7, 40, 0, zone);
        ZonedDateTime expected = ZonedDateTime.of(2026, 10, 27, 18, 15, 0, 0, zone);

        ZonedDateTime cut = BlockCutPolicy.nextCut(now, canalConDuracion(15));

        assertEquals(expected, cut);
    }

    @Test
    void respetaVentanaPermitidaSiOcurreAntesDelCorte() {
        // Ventana permitida que termina a las 18:09:00, antes del corte natural de las 18:10:00
        ExclusionRule allowedWindow = new ExclusionRule(null, LocalTime.of(5, 0), LocalTime.of(18, 9));
        Channel channel = new Channel(
                1L, "ADN40", "http://example.test/stream.m3u8",
                StreamProtocol.M3U8_HLS, true, OutputFormat.MKV,
                List.of(), List.of(), List.of(allowedWindow), 10
        );

        ZonedDateTime now = ZonedDateTime.of(2026, 10, 27, 18, 7, 40, 0, zone);
        ZonedDateTime expected = ZonedDateTime.of(2026, 10, 27, 18, 9, 0, 0, zone);

        ZonedDateTime cut = BlockCutPolicy.nextCut(now, channel);

        assertEquals(expected, cut);
    }

    @Test
    void respetaInicioDeExclusionSiOcurreAntesDelCorte() {
        // Regla de exclusion que inicia a las 18:08:00
        ExclusionRule exclusion = new ExclusionRule(null, LocalTime.of(18, 8), LocalTime.of(20, 0));
        Channel channel = new Channel(
                1L, "ADN40", "http://example.test/stream.m3u8",
                StreamProtocol.M3U8_HLS, true, OutputFormat.MKV,
                List.of(exclusion), List.of(), List.of(), 10
        );

        ZonedDateTime now = ZonedDateTime.of(2026, 10, 27, 18, 7, 40, 0, zone);
        ZonedDateTime expected = ZonedDateTime.of(2026, 10, 27, 18, 8, 0, 0, zone);

        ZonedDateTime cut = BlockCutPolicy.nextCut(now, channel);

        assertEquals(expected, cut);
    }
}
