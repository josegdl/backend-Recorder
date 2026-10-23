package com.recorder.backend.infrastructure.adapter.out.persistence;

import com.recorder.backend.domain.model.Channel;
import com.recorder.backend.domain.port.out.ChannelRepositoryPort;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Repositorio provisional en memoria. Se sustituira por el adaptador de
 * persistencia real cuando se implemente la capa de datos.
 */
public class InMemoryChannelRepositoryAdapter implements ChannelRepositoryPort {

    private final ConcurrentHashMap<Long, Channel> channels = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong();

    @Override
    public Optional<Channel> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(channels.get(id));
    }

    @Override
    public List<Channel> findAll() {
        return channels.values().stream()
                .sorted(Comparator.comparing(Channel::id))
                .toList();
    }

    @Override
    public Channel save(Channel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("El canal es obligatorio");
        }
        Long id = channel.id();
        if (id == null) {
            id = idGenerator.incrementAndGet();
        } else {
            idGenerator.accumulateAndGet(id, Math::max);
        }
        Channel stored = new Channel(id, channel.name(), channel.sourceUrl(),
                channel.protocol(), channel.enabled(), channel.format());
        channels.put(id, stored);
        return stored;
    }
}
