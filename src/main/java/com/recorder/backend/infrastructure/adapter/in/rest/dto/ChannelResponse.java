package com.recorder.backend.infrastructure.adapter.in.rest.dto;

import com.recorder.backend.domain.model.Channel;
import com.recorder.backend.domain.model.OutputFormat;
import com.recorder.backend.domain.model.StreamProtocol;

public record ChannelResponse(
        Long id,
        String name,
        String sourceUrl,
        StreamProtocol protocol,
        boolean enabled,
        OutputFormat format,
        int blockDurationMinutes
) {

    public static ChannelResponse from(Channel channel) {
        return new ChannelResponse(channel.id(), channel.name(), channel.sourceUrl(),
                channel.protocol(), channel.enabled(), channel.format(), channel.blockDurationMinutes());
    }
}
