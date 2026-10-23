package com.recorder.backend.domain.port.out;

import com.recorder.backend.domain.model.Channel;
import java.util.List;
import java.util.Optional;

public interface ChannelRepositoryPort {
    Optional<Channel> findById(Long id);
    List<Channel> findAll();
    Channel save(Channel channel);
}
