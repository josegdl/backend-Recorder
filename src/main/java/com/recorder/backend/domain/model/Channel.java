package com.recorder.backend.domain.model;

public record Channel(
    Long id,
    String name,
    String sourceUrl,
    StreamProtocol protocol,
    boolean enabled
) {}
