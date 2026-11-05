package com.recorder.backend.infrastructure.adapter.in.scheduler;

import com.recorder.backend.domain.exception.ChannelNotFoundException;
import com.recorder.backend.domain.model.Channel;
import com.recorder.backend.domain.port.in.RecordingServicePort;
import com.recorder.backend.domain.port.in.SchedulerServicePort;
import com.recorder.backend.domain.port.out.ChannelRepositoryPort;
import com.recorder.backend.domain.port.out.SchedulerPort;
import com.recorder.backend.domain.port.out.StreamSupervisorPort;
import com.recorder.backend.domain.service.BlockCutPolicy;
import com.recorder.backend.domain.service.HorarioValidator;
import com.recorder.backend.infrastructure.config.RecorderProperties;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SchedulerOrchestrator implements SchedulerServicePort {

    private static final Logger log = LoggerFactory.getLogger(SchedulerOrchestrator.class);

    private final ChannelRepositoryPort channelRepository;
    private final RecordingServicePort recordingService;
    private final StreamSupervisorPort supervisor;
    private final SchedulerPort schedulerPort;
    private final Clock clock;
    private final int tickSeconds;
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> blockTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ZonedDateTime> blockEnds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ZonedDateTime> blockStarts = new ConcurrentHashMap<>();
    private final Set<Long> activeChannels = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile ScheduledFuture<?> heartbeat;

    public SchedulerOrchestrator(ChannelRepositoryPort channelRepository,
                                 RecordingServicePort recordingService,
                                 StreamSupervisorPort supervisor,
                                 SchedulerPort schedulerPort,
                                 Clock clock,
                                 RecorderProperties properties) {
        this.channelRepository = channelRepository;
        this.recordingService = recordingService;
        this.supervisor = supervisor;
        this.schedulerPort = schedulerPort;
        this.clock = clock;
        this.tickSeconds = Math.max(1, properties.getScheduler().getTickSeconds());
    }

    @Override
    public synchronized void startScheduler() {
        if (running.get()) {
            return;
        }
        running.set(true);
        log.info("Iniciando scheduler global por bloques alineados al reloj (tick {}s)", tickSeconds);
        channelRepository.findAll().forEach(channel -> {
            if (channel.enabled()) {
                activeChannels.add(channel.id());
            }
        });
        reconcileAll();
        ensureHeartbeat();
    }

    @Override
    public synchronized void stopScheduler() {
        running.set(false);
        log.info("Deteniendo scheduler de forma ordenada");
        ScheduledFuture<?> beat = heartbeat;
        if (beat != null) {
            beat.cancel(true);
            heartbeat = null;
        }
        blockTasks.values().forEach(future -> {
            if (future != null) {
                future.cancel(true);
            }
        });
        blockTasks.clear();
        blockEnds.clear();
        blockStarts.clear();
        channelRepository.findAll().forEach(this::stopChannelQuietly);
        activeChannels.clear();
        schedulerPort.cancelAll();
        log.info("Scheduler detenido y canales marcados en INACTIVO");
    }

    @Override
    public synchronized void startChannel(Long channelId) {
        if (channelId == null) {
            throw new IllegalArgumentException("El id del canal es obligatorio");
        }
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ChannelNotFoundException(channelId));
        if (!channel.enabled()) {
            throw new IllegalStateException("El canal " + channelId + " esta deshabilitado");
        }
        activeChannels.add(channelId);
        log.info("Iniciando grabacion por bloques con emparejamiento para el canal {}", channelId);
        ensureHeartbeat();
        reconcileChannel(channel);
    }

    @Override
    public synchronized void stopChannel(Long channelId) {
        if (channelId == null) {
            throw new IllegalArgumentException("El id del canal es obligatorio");
        }
        activeChannels.remove(channelId);
        cancelBlockTask(channelId);
        channelRepository.findById(channelId).ifPresent(this::stopChannelQuietly);
        if (!running.get() && activeChannels.isEmpty()) {
            ScheduledFuture<?> beat = heartbeat;
            if (beat != null) {
                beat.cancel(true);
                heartbeat = null;
            }
        }
        log.info("Grabacion por bloques detenida para el canal {}", channelId);
    }

    @Override
    public boolean isChannelActive(Long channelId) {
        return activeChannels.contains(channelId) && supervisor.isRunning(channelId);
    }

    @Override
    public boolean isRunning() {
        return running.get() || !activeChannels.isEmpty();
    }

    private boolean isChannelScheduled(Long channelId) {
        return running.get() || activeChannels.contains(channelId);
    }

    private synchronized void reconcileAll() {
        if (running.get()) {
            channelRepository.findAll().forEach(this::reconcileChannel);
        } else {
            activeChannels.forEach(id -> channelRepository.findById(id).ifPresent(this::reconcileChannel));
        }
    }

    private void reconcileChannel(Channel channel) {
        if (channel == null || channel.id() == null || !isChannelScheduled(channel.id())) {
            return;
        }
        ZonedDateTime now = ZonedDateTime.now(clock);
        boolean operational = HorarioValidator.isOperational(now, channel);
        boolean active = supervisor.isRunning(channel.id());

        if (!operational) {
            if (active) {
                log.info("Canal {} fuera de horario valido; deteniendo grabacion", channel.id());
                stopChannelQuietly(channel);
            }
            cancelBlockTask(channel.id());
            blockStarts.remove(channel.id());
            return;
        }

        if (!active) {
            cancelBlockTask(channel.id());
            blockStarts.remove(channel.id());
            startBlock(channel, now);
            return;
        }

        if (!blockStarts.containsKey(channel.id())) {
            blockStarts.put(channel.id(), now);
            startBlock(channel, now);
            return;
        }

        ZonedDateTime trackedEnd = blockEnds.get(channel.id());
        if (trackedEnd != null && !now.isBefore(trackedEnd)) {
            log.info("Corte de bloque alcanzado para el canal {}", channel.id());
            startBlock(channel, now);
            return;
        }
        if (trackedEnd == null) {
            ZonedDateTime start = blockStarts.getOrDefault(channel.id(), now);
            ZonedDateTime cut = BlockCutPolicy.nextCut(start, channel);
            scheduleBlockEnd(channel, cut);
        }
    }

    private void startBlock(Channel channel, ZonedDateTime now) {
        stopChannelQuietly(channel);
        if (!isChannelScheduled(channel.id()) || !HorarioValidator.isOperational(now, channel)) {
            cancelBlockTask(channel.id());
            blockStarts.remove(channel.id());
            return;
        }
        ZonedDateTime startTime = now;
        blockStarts.put(channel.id(), startTime);
        ZonedDateTime cut = BlockCutPolicy.nextCut(startTime, channel);
        try {
            log.info("Iniciando bloque para canal {} desde {} hasta {}", channel.id(), startTime, cut);
            recordingService.startChannelRecording(channel.id());
        } catch (Exception e) {
            log.error("No se pudo iniciar grabacion para el canal {} en el bloque", channel.id(), e);
            return;
        }
        scheduleBlockEnd(channel, cut);
    }

    private void scheduleBlockEnd(Channel channel, ZonedDateTime end) {
        blockEnds.put(channel.id(), end);
        ScheduledFuture<?> previous = blockTasks.remove(channel.id());
        if (previous != null) {
            previous.cancel(true);
        }
        ScheduledFuture<?> future = schedulerPort.schedule(
                () -> onBlockEnd(channel, end), end.toInstant());
        if (future != null) {
            blockTasks.put(channel.id(), future);
        }
    }

    private synchronized void onBlockEnd(Channel channel, ZonedDateTime scheduledEnd) {
        if (!isChannelScheduled(channel.id())) {
            return;
        }
        ZonedDateTime trackedEnd = blockEnds.get(channel.id());
        if (trackedEnd == null || !trackedEnd.equals(scheduledEnd)) {
            return;
        }
        log.info("Finalizando bloque por duracion/limite del canal {}", channel.id());
        startBlock(channel, ZonedDateTime.now(clock));
    }

    private void scheduleHeartbeat() {
        if (!running.get() && activeChannels.isEmpty()) {
            heartbeat = null;
            return;
        }
        ZonedDateTime next = ZonedDateTime.now(clock).plusSeconds(tickSeconds);
        ScheduledFuture<?> future = schedulerPort.schedule(() -> {
            try {
                if (running.get() || !activeChannels.isEmpty()) {
                    reconcileAll();
                }
            } catch (Exception e) {
                log.error("Error al reconciliar el estado del scheduler", e);
            } finally {
                if (running.get() || !activeChannels.isEmpty()) {
                    scheduleHeartbeat();
                } else {
                    heartbeat = null;
                }
            }
        }, next.toInstant());
        heartbeat = future;
    }

    private synchronized void ensureHeartbeat() {
        ScheduledFuture<?> beat = heartbeat;
        if (beat == null || beat.isDone() || beat.isCancelled()) {
            scheduleHeartbeat();
        }
    }

    private void cancelBlockTask(Long channelId) {
        ScheduledFuture<?> future = blockTasks.remove(channelId);
        if (future != null) {
            future.cancel(true);
        }
        blockEnds.remove(channelId);
        blockStarts.remove(channelId);
    }

    private void stopChannelQuietly(Channel channel) {
        try {
            if (supervisor.isRunning(channel.id())) {
                recordingService.stopChannelRecording(channel.id());
            }
        } catch (Exception e) {
            log.warn("Error al detener el canal {} durante la reconciliacion", channel.id(), e);
        }
    }

    @PreDestroy
    public synchronized void destroy() {
        if (running.get() || !activeChannels.isEmpty()) {
            stopScheduler();
        }
    }
}
