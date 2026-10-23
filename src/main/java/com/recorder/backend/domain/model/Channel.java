package com.recorder.backend.domain.model;

public record Channel(
    Long id,
    String name,
    String sourceUrl,
    StreamProtocol protocol,
    boolean enabled,
    OutputFormat format
) {

    public Channel(Long id, String name, String sourceUrl, StreamProtocol protocol, boolean enabled) {
        this(id, name, sourceUrl, protocol, enabled, OutputFormat.MKV);
    }
}