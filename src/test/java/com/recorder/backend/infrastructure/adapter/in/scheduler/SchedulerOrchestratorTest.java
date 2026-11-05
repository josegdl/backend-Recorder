package com.recorder.backend.infrastructure.adapter.in.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recorder.backend.domain.exception.ChannelNotFoundException;
import com.recorder.backend.domain.model.Channel;
import com.recorder.backend.domain.model.OutputFormat;
import com.recorder.backend.domain.model.StreamProtocol;
import com.recorder.backend.domain.port.in.RecordingServicePort;
import com.recorder.backend.domain.port.out.ChannelRepositoryPort;
import com.recorder.backend.domain.port.out.SchedulerPort;
import com.recorder.backend.domain.port.out.StreamSupervisorPort;
import com.recorder.backend.infrastructure.config.RecorderProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SchedulerOrchestratorTest {

    private final ChannelRepositoryPort channelRepository = mock(ChannelRepositoryPort.class);
    private final RecordingServicePort recordingService = mock(RecordingServicePort.class);
    private final StreamSupervisorPort supervisor = mock(StreamSupervisorPort.class);
    private final SchedulerPort schedulerPort = mock(SchedulerPort.class);
    private final ZoneId zone = ZoneId.of("UTC");
    private final ZonedDateTime fixedTime = ZonedDateTime.of(2026, 10, 27, 18, 7, 40, 0, zone);
    private final Clock clock = Clock.fixed(fixedTime.toInstant(), zone);
    private final RecorderProperties properties = new RecorderProperties();

    private SchedulerOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new SchedulerOrchestrator(
                channelRepository, recordingService, supervisor, schedulerPort, clock, properties
        );
    }

    private Channel canal(Long id, boolean enabled, int durationMinutes) {
        return new Channel(
                id, "ADN40", "http://example.test/stream.m3u8",
                StreamProtocol.M3U8_HLS, enabled, OutputFormat.MKV,
                List.of(), List.of(), List.of(), durationMinutes
        );
    }

    @Test
    void startChannelIniciaBloqueYProgramaCorteConEmparejamiento() {
        Channel ch = canal(1L, true, 10);
        when(channelRepository.findById(1L)).thenReturn(Optional.of(ch));
        when(supervisor.isRunning(1L)).thenReturn(false).thenReturn(true);

        orchestrator.startChannel(1L);

        verify(recordingService).startChannelRecording(1L);

        // A las 18:07:40 con 10 min, el corte debe programarse exactamente para las 18:10:00 UTC
        Instant expectedCutInstant = ZonedDateTime.of(2026, 10, 27, 18, 10, 0, 0, zone).toInstant();
        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(schedulerPort).schedule(any(Runnable.class), instantCaptor.capture());

        assertEquals(expectedCutInstant, instantCaptor.getValue());
        assertTrue(orchestrator.isRunning());
    }

    @Test
    void stopChannelDetieneGrabacionYCancelaTareas() {
        Channel ch = canal(1L, true, 10);
        when(channelRepository.findById(1L)).thenReturn(Optional.of(ch));
        when(supervisor.isRunning(1L)).thenReturn(true);

        orchestrator.startChannel(1L);
        orchestrator.stopChannel(1L);

        verify(recordingService).stopChannelRecording(1L);
    }

    @Test
    void startChannelLanzaExcepcionSiCanalNoExisteODeshabilitado() {
        when(channelRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ChannelNotFoundException.class, () -> orchestrator.startChannel(99L));

        Channel disabled = canal(2L, false, 10);
        when(channelRepository.findById(2L)).thenReturn(Optional.of(disabled));
        assertThrows(IllegalStateException.class, () -> orchestrator.startChannel(2L));
    }
}
